package utils;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.Objects;

public class MultiScaleTemplateMatcher {

    public static class Match {
        public final double score;        // 0..1 (TM_CCOEFF_NORMED: higher is better)
        public final Point topLeft;       // in haystack coordinates
        public final Rect location;       // bounding rect in haystack
        public final double scaleUsed;    // scale applied to the needle

        public Match(double score, Point topLeft, Rect location, double scaleUsed) {
            this.score = score;
            this.topLeft = topLeft;
            this.location = location;
            this.scaleUsed = scaleUsed;
        }

        @Override
        public String toString() {
            return String.format(
                    "Match{score=%.4f, topLeft=(%.1f,%.1f), rect=%s, scale=%.2f}",
                    score, topLeft.x, topLeft.y, location.size(), scaleUsed
            );
        }
    }

    static {
        try {
            // Try nu.pattern helper first (if present)
            nu.pattern.OpenCV.loadShared();
        } catch (Throwable t) {
            // Fallback to the default loader
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        }
    }

    /**
     * Multi-scale template matching (downscales the needle until it fits the haystack).
     * Uses TM_CCOEFF_NORMED for matching.
     *
     * @param haystackBgr Full/large scene image (BGR)
     * @param needleBgr   Template/fragment image (BGR)
     * @param maxScale    Starting scale for the needle (e.g., 1.0)
     * @param minScale    Minimum scale to try (e.g., 0.3)
     * @param step        Decrement step (e.g., 0.05)
     * @param blurKsize   Optional Gaussian blur kernel for noise reduction (e.g., 3; use 0 to skip)
     * @return Match with best score & location (throws if no feasible scale found)
     */
    public static Match matchTemplateMultiScale(
            Mat haystackBgr,
            Mat needleBgr,
            double maxScale,
            double minScale,
            double step,
            int blurKsize
    ) {

        validate(haystackBgr, "haystackBgr");
        validate(needleBgr, "needleBgr");

        // Pre-convert haystack to grayscale (+ optional blur) once
        Mat hayGray = new Mat();
        Imgproc.cvtColor(haystackBgr, hayGray, Imgproc.COLOR_BGR2GRAY);
        if (blurKsize >= 3 && blurKsize % 2 == 1) {
            Imgproc.GaussianBlur(hayGray, hayGray, new Size(blurKsize, blurKsize), 0);
        }

        double bestScore = -1.0;
        Point bestTopLeft = null;
        Rect bestRect = null;
        double bestScale = 1.0;

        // Iterate scales from max -> min
        for (double scale = maxScale; scale >= minScale; scale -= step) {
            int w = (int) Math.round(needleBgr.cols() * scale);
            int h = (int) Math.round(needleBgr.rows() * scale);
            if (w < 5 || h < 5) break; // too small to be meaningful

            // Skip if still larger than haystack
            if (w > hayGray.cols() || h > hayGray.rows()) continue;

            // Prepare scaled needle grayscale (+ optional blur)
            Mat scaled = new Mat();
            Imgproc.resize(needleBgr, scaled, new Size(w, h), 0, 0, Imgproc.INTER_AREA);

            Mat neeGray = new Mat();
            Imgproc.cvtColor(scaled, neeGray, Imgproc.COLOR_BGR2GRAY);
            if (blurKsize >= 3 && blurKsize % 2 == 1) {
                Imgproc.GaussianBlur(neeGray, neeGray, new Size(blurKsize, blurKsize), 0);
            }

            // Ensure result matrix has valid size
            int resRows = hayGray.rows() - neeGray.rows() + 1;
            int resCols = hayGray.cols() - neeGray.cols() + 1;
            if (resRows <= 0 || resCols <= 0) continue;

            Mat result = new Mat(resRows, resCols, CvType.CV_32FC1);
            Imgproc.matchTemplate(hayGray, neeGray, result, Imgproc.TM_CCOEFF_NORMED);

            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            double score = mmr.maxVal;
            Point topLeft = mmr.maxLoc;
            Rect rect = new Rect(topLeft, new Size(neeGray.cols(), neeGray.rows()));

            if (score > bestScore) {
                bestScore = score;
                bestTopLeft = topLeft;
                bestRect = rect;
                bestScale = scale;
            }
        }

        if (bestRect == null) {
            throw new IllegalArgumentException(
                    "matchTemplateMultiScale: no feasible scale found where needle fits haystack. " +
                            "haystack=" + haystackBgr.size() + ", needle=" + needleBgr.size()
            );
        }

        return new Match(bestScore, bestTopLeft, bestRect, bestScale);
    }

    /** Convenience: single-scale direct match (needle must be <= haystack). */
    public static Match matchTemplate(Mat haystackBgr, Mat needleBgr, int blurKsize) {
        validate(haystackBgr, "haystackBgr");
        validate(needleBgr, "needleBgr");

        if (needleBgr.cols() > haystackBgr.cols() || needleBgr.rows() > haystackBgr.rows()) {
            throw new IllegalArgumentException(
                    "matchTemplate: needle bigger than haystack. haystack=" +
                            haystackBgr.size() + " needle=" + needleBgr.size()
            );
        }

        Mat hayGray = new Mat();
        Mat neeGray = new Mat();
        Imgproc.cvtColor(haystackBgr, hayGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(needleBgr, neeGray, Imgproc.COLOR_BGR2GRAY);

        if (blurKsize >= 3 && blurKsize % 2 == 1) {
            Imgproc.GaussianBlur(hayGray, hayGray, new Size(blurKsize, blurKsize), 0);
            Imgproc.GaussianBlur(neeGray, neeGray, new Size(blurKsize, blurKsize), 0);
        }

        int resRows = hayGray.rows() - neeGray.rows() + 1;
        int resCols = hayGray.cols() - neeGray.cols() + 1;
        if (resRows <= 0 || resCols <= 0) {
            throw new IllegalArgumentException(
                    "matchTemplate: invalid result size. haystack=" +
                            hayGray.size() + ", needle=" + neeGray.size()
            );
        }

        Mat result = new Mat(resRows, resCols, CvType.CV_32FC1);
        Imgproc.matchTemplate(hayGray, neeGray, result, Imgproc.TM_CCOEFF_NORMED);

        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        Point topLeft = mmr.maxLoc;
        double score = mmr.maxVal;
        Rect rect = new Rect(topLeft, new Size(neeGray.cols(), neeGray.rows()));

        return new Match(score, topLeft, rect, 1.0);
    }

    private static void validate(Mat m, String name) {
        if (m == null || m.empty()) {
            throw new IllegalArgumentException(name + " is null/empty");
        }
        if (m.cols() <= 0 || m.rows() <= 0) {
            throw new IllegalArgumentException(name + " has invalid size: " + m.size());
        }
    }
}