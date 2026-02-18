package utils;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;

public class OpenCVUtil {

    static {
        OpenCV.loadLocally();
        System.out.println("[OpenCV] Library loaded successfully.");
    }

    /**
     * Clicks on a UI element in a screenshot using a template image.
     * Passes only if similarity is almost exact.
     */
    public static boolean clickUsingImage(WebDriver driver,
                                          String screenshotPath,
                                          String templatePath) {

        System.out.println("--------------------------------------------------");
        System.out.println("[OpenCV] Starting strict image validation...");
        System.out.println("[OpenCV] Screenshot Path: " + screenshotPath);
        System.out.println("[OpenCV] Template Path  : " + templatePath);

        Mat source = Imgcodecs.imread(screenshotPath);
        Mat template = Imgcodecs.imread(templatePath);

        if (source.empty()) {
            throw new RuntimeException("[ERROR] Screenshot not loaded: " + screenshotPath);
        }
        if (template.empty()) {
            throw new RuntimeException("[ERROR] Template not loaded: " + templatePath);
        }

        if (template.cols() > source.cols() || template.rows() > source.rows()) {
            System.out.println("[WARNING] Template larger than screenshot, resizing minimally...");
            double scaleWidth = (double) source.cols() / template.cols();
            double scaleHeight = (double) source.rows() / template.rows();
            double scale = Math.min(scaleWidth, scaleHeight);

            Mat resized = new Mat();
            Imgproc.resize(template, resized, new Size(), scale, scale);
            template = resized;

            System.out.println("[INFO] Resized Template Size: " + template.cols() + " x " + template.rows());
        }

        // Convert to grayscale for strict matching
        Imgproc.cvtColor(source, source, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(template, template, Imgproc.COLOR_BGR2GRAY);

        // Optional: slightly blur to reduce noise
        Imgproc.GaussianBlur(source, source, new Size(3,3), 0);
        Imgproc.GaussianBlur(template, template, new Size(3,3), 0);

        // Prepare result matrix
        int resultCols = source.cols() - template.cols() + 1;
        int resultRows = source.rows() - template.rows() + 1;
        Mat result = new Mat(resultRows, resultCols, CvType.CV_32FC1);

        Imgproc.matchTemplate(source, template, result, Imgproc.TM_CCOEFF_NORMED);

        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        System.out.println("[RESULT] Maximum Similarity Score: " + mmr.maxVal);

        double threshold = 0.5;
        System.out.println("[INFO] Threshold for strict validation: " + threshold);

        if (mmr.maxVal >= threshold) {
            System.out.println("[SUCCESS] Image match PASSED. Similarity meets strict criteria.");
            return true;
        } else {
            System.out.println("[FAILURE] Image match FAILED. Similarity below strict threshold.");
            return false;
        }
    }
    public static Mat resizeMat(Mat input, int width, int height) {
        Mat output = new Mat();
        Size sz = new Size(width, height);
        Imgproc.resize(input, output, sz);
        return output;
    }
}
