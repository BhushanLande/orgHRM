package utils;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class VisualAssert {

    static {
        try {
            nu.pattern.OpenCV.loadShared();
        } catch (Throwable t) {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        }
    }

    // ====================== 3.1 Template Matching (presence + location) ======================
    public static class TemplateResult {
        public final double score; // 0..1 (higher is better for TM_CCOEFF_NORMED)
        public final Point topLeft;
        public final Rect location;

        TemplateResult(double score, Point topLeft, Rect location) {
            this.score = score;
            this.topLeft = topLeft;
            this.location = location;
        }
    }

    public static TemplateResult matchTemplate(Mat haystackBgr, Mat needleBgr) {
        // Defaults tuned for common DPR/viewports
        double maxScale = 1.0; // try original size first
        double minScale = 0.35; // go down to 35%
        double step = 0.05; // in 5% decrements
        int blurKsize = 3; // Gaussian blur kernel (use 0 to skip)

        return matchTemplateMultiScale(haystackBgr, needleBgr, maxScale, minScale, step, blurKsize);
    }

    public static TemplateResult matchTemplateMultiScale(Mat haystackBgr,
                                                         Mat needleBgr,
                                                         double maxScale,
                                                         double minScale,
                                                         double step,
                                                         int blurKsize) {
        if (haystackBgr == null || haystackBgr.empty()) {
            throw new IllegalArgumentException("matchTemplateMultiScale: haystackBgr is empty/null. " +
                    "Check screenshot capture or image read path.");
        }
        if (needleBgr == null || needleBgr.empty()) {
            throw new IllegalArgumentException("matchTemplateMultiScale: needleBgr is empty/null. " +
                    "Make sure your fragment/baseline exists and path is correct.");
        }
        if (maxScale <= 0 || minScale <= 0 || step <= 0 || maxScale < minScale) {
            throw new IllegalArgumentException("Invalid scales: maxScale=" + maxScale +
                    ", minScale=" + minScale + ", step=" + step);
        }

        // Pre-convert haystack to grayscale (+ optional blur) once
        Mat hayGray = new Mat();
        Imgproc.cvtColor(haystackBgr, hayGray, Imgproc.COLOR_BGR2GRAY);
        if (blurKsize >= 3 && (blurKsize % 2 == 1)) {
            Imgproc.GaussianBlur(hayGray, hayGray, new Size(blurKsize, blurKsize), 0);
        }

        double bestScore = -1.0;
        Point bestTopLeft = null;
        Rect bestRect = null;

        // Iterate scales from max -> min (downscale the needle until it fits)
        for (double scale = maxScale; scale >= minScale; scale -= step) {
            int w = (int) Math.round(needleBgr.cols() * scale);
            int h = (int) Math.round(needleBgr.rows() * scale);

            // Skip too small (meaningless template)
            if (w < 5 || h < 5) break;

            // Skip if still larger than haystack
            if (w > hayGray.cols() || h > hayGray.rows()) continue;

            // Prepare scaled needle grayscale (+ optional blur)
            Mat scaled = new Mat();
            Imgproc.resize(needleBgr, scaled, new Size(w, h), 0, 0, Imgproc.INTER_AREA);

            Mat neeGray = new Mat();
            Imgproc.cvtColor(scaled, neeGray, Imgproc.COLOR_BGR2GRAY);
            if (blurKsize >= 3 && (blurKsize % 2 == 1)) {
                Imgproc.GaussianBlur(neeGray, neeGray, new Size(blurKsize, blurKsize), 0);
            }

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
            }
        }

        if (bestRect == null) {
            // Optional: dump artifacts to debug quickly
            // Imgcodecs.imwrite("artifacts/haystack.png", haystackBgr);
            // Imgcodecs.imwrite("artifacts/needle.png", needleBgr);
            throw new IllegalArgumentException(
                    "matchTemplateMultiScale: no feasible scale found where needle fits haystack. " +
                            "haystack=" + haystackBgr.size() + ", needle=" + needleBgr.size()
            );
        }

        return new TemplateResult(bestScore, bestTopLeft, bestRect);
    }

    public static Mat toMat(BufferedImage bi) {
        if (bi.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            BufferedImage bgr = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = bgr.createGraphics();
            g.drawImage(bi, 0, 0, null);
            g.dispose();
            bi = bgr;
        }
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }
}