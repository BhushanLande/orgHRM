package utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class VisualAssert {

    static {
        // Load OpenCV native library once.
        // Preferred (if you added dependency 'nu.pattern:opencv'):
        try {
            nu.pattern.OpenCV.loadShared();
        } catch (Throwable t) {
            // Fallback to standard loader if helper not present
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        }
    }

    // ====================== 3.1 Template Matching (presence + location) ======================
    public static class TemplateResult {
        public final double score;        // 0..1 (higher is better for TM_CCOEFF_NORMED)
        public final Point topLeft;
        public final Rect location;

        TemplateResult(double score, Point topLeft, Rect location) {
            this.score = score;
            this.topLeft = topLeft;
            this.location = location;
        }
    }

    // ====================== Multi-scale Template Matching ======================

    /**
     * Backward-compatible method name that now performs multi-scale matching
     * with sensible defaults (maxScale=1.0, minScale=0.35, step=0.05, blur=3).
     *
     * It will downscale the needle (template) until it fits the haystack,
     * and return the best match (highest TM_CCOEFF_NORMED score).
     */
    public static TemplateResult matchTemplate(Mat haystackBgr, Mat needleBgr) {
        // Defaults tuned for common DPR/viewports
        double maxScale = 1.0;   // try original size first
        double minScale = 0.35;  // go down to 35%
        double step = 0.05;      // in 5% decrements
        int blurKsize = 3;       // Gaussian blur kernel (use 0 to skip)

        return matchTemplateMultiScale(
                haystackBgr, needleBgr, maxScale, minScale, step, blurKsize
        );
    }

    /**
     * Multi-scale template matching (downscales the needle until it fits the haystack).
     * Uses TM_CCOEFF_NORMED for matching.
     *
     * @param haystackBgr Full/large scene image (BGR)
     * @param needleBgr   Template/fragment image (BGR)
     * @param maxScale    Starting scale for the needle (e.g., 1.0)
     * @param minScale    Minimum scale to try (e.g., 0.35)
     * @param step        Decrement step (e.g., 0.05)
     * @param blurKsize   Optional Gaussian blur kernel for noise reduction (>=3 and odd), 0 to skip
     * @return TemplateResult with best score & location (throws if no feasible scale found)
     */
    public static TemplateResult matchTemplateMultiScale(
            Mat haystackBgr,
            Mat needleBgr,
            double maxScale,
            double minScale,
            double step,
            int blurKsize
    ) {

        if (haystackBgr == null || haystackBgr.empty()) {
            throw new IllegalArgumentException(
                    "matchTemplateMultiScale: haystackBgr is empty/null. " +
                            "Check screenshot capture or image read path."
            );
        }
        if (needleBgr == null || needleBgr.empty()) {
            throw new IllegalArgumentException(
                    "matchTemplateMultiScale: needleBgr is empty/null. " +
                            "Make sure your fragment/baseline exists and path is correct."
            );
        }
        if (maxScale <= 0 || minScale <= 0 || step <= 0 || maxScale < minScale) {
            throw new IllegalArgumentException(
                    "Invalid scales: maxScale=" + maxScale +
                            ", minScale=" + minScale + ", step=" + step
            );
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
            throw new IllegalArgumentException(
                    "matchTemplateMultiScale: no feasible scale found where needle fits haystack. " +
                            "haystack=" + haystackBgr.size() + ", needle=" + needleBgr.size()
            );
        }

        return new TemplateResult(bestScore, bestTopLeft, bestRect);
    }

    // ===== 3.2 ORB Feature Matching + Homography (robust across scale/rotation) =====
    public static class OrbResult {
        public final double inlierRatio;  // 0..1
        public final Mat homography;      // can be null if not found

        OrbResult(double inlierRatio, Mat H) {
            this.inlierRatio = inlierRatio;
            this.homography = H;
        }
    }

    public static OrbResult orbHomography(Mat sceneBgr, Mat objectBgr) {

        Mat sceneGray = new Mat();
        Mat objGray = new Mat();
        Imgproc.cvtColor(sceneBgr, sceneGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(objectBgr, objGray, Imgproc.COLOR_BGR2GRAY);

        ORB orb = ORB.create(1000);

        org.opencv.core.MatOfKeyPoint kpScene = new org.opencv.core.MatOfKeyPoint();
        org.opencv.core.MatOfKeyPoint kpObj = new org.opencv.core.MatOfKeyPoint();
        Mat descScene = new Mat();
        Mat descObj = new Mat();

        orb.detectAndCompute(sceneGray, new Mat(), kpScene, descScene);
        orb.detectAndCompute(objGray, new Mat(), kpObj, descObj);

        if (descScene.empty() || descObj.empty()) {
            return new OrbResult(0.0, null);
        }

        DescriptorMatcher matcher = BFMatcher.create(Core.NORM_HAMMING, false);
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(descObj, descScene, knnMatches, 2);

        // Lowe’s ratio test
        final float ratioThresh = 0.75f;
        List<DMatch> good = new ArrayList<>();

        for (MatOfDMatch m : knnMatches) {
            DMatch[] arr = m.toArray();
            if (arr.length >= 2 && arr[0].distance < ratioThresh * arr[1].distance) {
                good.add(arr[0]);
            }
        }

        if (good.size() < 6) {
            return new OrbResult(0.0, null);
        }

        List<Point> objPts = new ArrayList<>();
        List<Point> scenePts = new ArrayList<>();

        KeyPoint[] kpo = kpObj.toArray();
        KeyPoint[] kps = kpScene.toArray();

        for (DMatch m : good) {
            objPts.add(kpo[m.queryIdx].pt);
            scenePts.add(kps[m.trainIdx].pt);
        }

        MatOfPoint2f objMat = new MatOfPoint2f();
        objMat.fromList(objPts);

        MatOfPoint2f sceneMat = new MatOfPoint2f();
        sceneMat.fromList(scenePts);

        Mat mask = new Mat();
        Mat H = Calib3d.findHomography(objMat, sceneMat, Calib3d.RANSAC, 3.0, mask);

        int inliers = 0;
        for (int i = 0; i < mask.rows(); i++) {
            if (mask.get(i, 0)[0] == 1.0) {
                inliers++;
            }
        }

        double inlierRatio = good.isEmpty() ? 0.0 : (double) inliers / good.size();
        return new OrbResult(inlierRatio, H);
    }

    // ====================== 3.3 SSIM (structural similarity) ======================
    public static double ssim(Mat img1Bgr, Mat img2Bgr) {

        Mat g1 = new Mat();
        Mat g2 = new Mat();
        Imgproc.cvtColor(img1Bgr, g1, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(img2Bgr, g2, Imgproc.COLOR_BGR2GRAY);

        if (!g1.size().equals(g2.size())) {
            Imgproc.resize(g2, g2, g1.size());
        }

        g1.convertTo(g1, CvType.CV_32F);
        g2.convertTo(g2, CvType.CV_32F);

        Mat mu1 = new Mat();
        Mat mu2 = new Mat();
        Imgproc.GaussianBlur(g1, mu1, new Size(11, 11), 1.5);
        Imgproc.GaussianBlur(g2, mu2, new Size(11, 11), 1.5);

        Mat mu1_2 = new Mat();
        Core.multiply(mu1, mu1, mu1_2);

        Mat mu2_2 = new Mat();
        Core.multiply(mu2, mu2, mu2_2);

        Mat mu1_mu2 = new Mat();
        Core.multiply(mu1, mu2, mu1_mu2);

        Mat g1_2 = new Mat();
        Core.multiply(g1, g1, g1_2);

        Mat g2_2 = new Mat();
        Core.multiply(g2, g2, g2_2);

        Mat g1g2 = new Mat();
        Core.multiply(g1, g2, g1g2);

        Mat sigma1_2 = new Mat();
        Imgproc.GaussianBlur(g1_2, sigma1_2, new Size(11, 11), 1.5);
        Core.subtract(sigma1_2, mu1_2, sigma1_2);

        Mat sigma2_2 = new Mat();
        Imgproc.GaussianBlur(g2_2, sigma2_2, new Size(11, 11), 1.5);
        Core.subtract(sigma2_2, mu2_2, sigma2_2);

        Mat sigma12 = new Mat();
        Imgproc.GaussianBlur(g1g2, sigma12, new Size(11, 11), 1.5);
        Core.subtract(sigma12, mu1_mu2, sigma12);

        double L = 255.0, K1 = 0.01, K2 = 0.03;
        double C1 = (K1 * L) * (K1 * L);
        double C2 = (K2 * L) * (K2 * L);

        Mat twoMu1Mu2 = new Mat();
        Core.multiply(mu1_mu2, new Scalar(2.0), twoMu1Mu2);

        Mat t1 = new Mat();
        Core.add(twoMu1Mu2, new Scalar(C1), t1);

        Mat twoSigma12 = new Mat();
        Core.multiply(sigma12, new Scalar(2.0), twoSigma12);

        Mat t2 = new Mat();
        Core.add(twoSigma12, new Scalar(C2), t2);

        Mat numerator = new Mat();
        Core.multiply(t1, t2, numerator);

        Mat muSumSq = new Mat();
        Core.add(mu1_2, mu2_2, muSumSq);

        Mat t3 = new Mat();
        Core.add(muSumSq, new Scalar(C1), t3);

        Mat sigmaSum = new Mat();
        Core.add(sigma1_2, sigma2_2, sigmaSum);

        Mat t4 = new Mat();
        Core.add(sigmaSum, new Scalar(C2), t4);

        Mat denominator = new Mat();
        Core.multiply(t3, t4, denominator);

        Mat ssimMap = new Mat();
        Core.divide(numerator, denominator, ssimMap);

        Scalar mean = Core.mean(ssimMap);
        return mean.val[0]; // 1.0 = identical
    }

    // ====================== 3.4 Perceptual difference via dHash (fast smoke) ======================
    public static long dHash64(Mat bgr) {

        Mat gray = new Mat();
        Imgproc.cvtColor(bgr, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.resize(gray, gray, new Size(9, 8));

        long hash = 0L;
        int bit = 0;

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++, bit++) {
                double left = gray.get(y, x)[0];
                double right = gray.get(y, x + 1)[0];
                if (left > right) {
                    hash |= (1L << bit);
                }
            }
        }

        return hash;
    }

    public static int hamming(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    // ====================== 3.5 Color/Theme validation via HSV histogram ======================
    public static double hsvCorrelation(Mat bgr1, Mat bgr2) {

        Mat hsv1 = new Mat();
        Mat hsv2 = new Mat();
        Imgproc.cvtColor(bgr1, hsv1, Imgproc.COLOR_BGR2HSV);
        Imgproc.cvtColor(bgr2, hsv2, Imgproc.COLOR_BGR2HSV);

        Mat hist1 = new Mat();
        Mat hist2 = new Mat();

        MatOfInt histChannels = new MatOfInt(0, 1); // H and S channels
        MatOfInt histSize = new MatOfInt(50, 60);
        MatOfFloat ranges = new MatOfFloat(0f, 180f, 0f, 256f);

        Imgproc.calcHist(
                Collections.singletonList(hsv1),
                histChannels,
                new Mat(),
                hist1,
                histSize,
                ranges
        );
        Imgproc.calcHist(
                Collections.singletonList(hsv2),
                histChannels,
                new Mat(),
                hist2,
                histSize,
                ranges
        );

        Core.normalize(hist1, hist1, 0, 1, Core.NORM_MINMAX);
        Core.normalize(hist2, hist2, 0, 1, Core.NORM_MINMAX);

        // If your OpenCV version uses HISTCMP_CORREL, switch constant below accordingly.
        return Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_CORREL); // 1.0 ~ identical
    }

    // ====================== 3.6 Visual Diff + Heatmap (with mask) ======================
    public static class DiffResult {
        public final Mat diffBgr;   // colored heatmap overlay
        public final double changedPct;

        DiffResult(Mat diffBgr, double changedPct) {
            this.diffBgr = diffBgr;
            this.changedPct = changedPct;
        }
    }

    /**
     * @param maskGray 0 = ignore; 255 = check
     */
    public static DiffResult diffWithMask(Mat actualBgr, Mat baselineBgr, Mat maskGray) {

        Mat a = actualBgr.clone();
        Mat b = baselineBgr.clone();

        if (!a.size().equals(b.size())) {
            Imgproc.resize(b, b, a.size());
        }

        Mat diff = new Mat();
        Core.absdiff(a, b, diff);

        // Convert to grayscale difference magnitude
        Mat diffGray = new Mat();
        Imgproc.cvtColor(diff, diffGray, Imgproc.COLOR_BGR2GRAY);

        if (maskGray != null && !maskGray.empty()) {
            if (!maskGray.size().equals(diffGray.size())) {
                Imgproc.resize(maskGray, maskGray, diffGray.size());
            }
            Core.bitwise_and(diffGray, maskGray, diffGray);
        }

        // Threshold small noise
        Imgproc.threshold(diffGray, diffGray, 25, 255, Imgproc.THRESH_BINARY);

        double changed = Core.countNonZero(diffGray);
        double total = (double) diffGray.rows() * diffGray.cols();
        double changedPct = (changed / total) * 100.0;

        // Create red heatmap overlay
        Mat heat = new Mat(a.size(), a.type(), Scalar.all(0));
        List<Mat> splitChannels = new ArrayList<>();
        Core.split(heat, splitChannels);   // B, G, R
        splitChannels.set(2, diffGray);    // use red channel for the mask
        Core.merge(splitChannels, heat);

        Mat overlay = new Mat();
        Core.addWeighted(a, 0.7, heat, 0.6, 0.0, overlay);

        return new DiffResult(overlay, changedPct);
    }

    // ====================== 3.7 WCAG Color Contrast check ======================
    public static double wcagContrastRatio(Color fg, Color bg) {

        double L1 = relativeLuminance(fg);
        double L2 = relativeLuminance(bg);

        double lighter = Math.max(L1, L2);
        double darker = Math.min(L1, L2);

        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(Color c) {

        double[] rgb = {
                c.getRed() / 255.0,
                c.getGreen() / 255.0,
                c.getBlue() / 255.0
        };

        for (int i = 0; i < 3; i++) {
            rgb[i] = (rgb[i] <= 0.03928)
                    ? (rgb[i] / 12.92)
                    : Math.pow((rgb[i] + 0.055) / 1.055, 2.4);
        }

        return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
    }

    public static Mat loadTemplateResized(String templatePath, Mat haystack) {
        Mat template = Imgcodecs.imread(templatePath);
        if (template.empty()) {
            throw new IllegalArgumentException("Template image not found: " + templatePath);
        }

        // Check if template is larger than haystack
        if (template.width() > haystack.width() || template.height() > haystack.height()) {
            double scaleWidth = (double) haystack.width() / template.width();
            double scaleHeight = (double) haystack.height() / template.height();
            double scale = Math.min(scaleWidth, scaleHeight); // scale to fit

            Size newSize = new Size(template.width() * scale, template.height() * scale);
            Mat resizedTemplate = new Mat();
            Imgproc.resize(template, resizedTemplate, newSize);
            return resizedTemplate;
        }

        return template;
    }
}
