package utils;

import org.openqa.selenium.*;
import java.io.File;
import org.apache.commons.io.FileUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.awt.image.*;
import java.awt.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

public class ScreenshotUtil {

    public static String captureScreenshot(WebDriver driver, String fileName) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String path = "screenshots/" + fileName + ".png";
            FileUtils.copyFile(src, new File(path));
            return path;
        } catch (Exception e) {
            throw new RuntimeException("Screenshot failed");
        }
    }

    public static String saveSnapshot(WebDriver driver, String fileName) {
        try {
            // Take screenshot as FILE
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            // Target directory inside src/main/resources/Snapshots
            String folder = "src/main/resources/Snapshots";
            File dir = new File(folder);

            // Create folder if not exists
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Build final path
            String finalPath = folder + "/" + fileName + ".png";
            File dest = new File(finalPath);

            // Copy screenshot to destination
            org.apache.commons.io.FileUtils.copyFile(src, dest);

            System.out.println("Saved snapshot: " + dest.getAbsolutePath());
            return dest.getAbsolutePath();

        } catch (Exception e) {
            throw new RuntimeException("Failed to save snapshot: " + e.getMessage(), e);
        }
    }

    public static BufferedImage fullPageScreenshot(WebDriver driver) throws Exception {
        byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    public static BufferedImage elementScreenshot(WebDriver driver, WebElement el) throws Exception {
        BufferedImage full = fullPageScreenshot(driver);
        double dpr = devicePixelRatio(driver);
        Point loc = el.getLocation();
        Dimension size = el.getSize();

        int x = (int) Math.round(loc.getX() * dpr);
        int y = (int) Math.round(loc.getY() * dpr);
        int w = (int) Math.round(size.getWidth() * dpr);
        int h = (int) Math.round(size.getHeight() * dpr);

        x = Math.max(0, Math.min(x, full.getWidth() - 1));
        y = Math.max(0, Math.min(y, full.getHeight() - 1));
        w = Math.max(1, Math.min(w, full.getWidth() - x));
        h = Math.max(1, Math.min(h, full.getHeight() - y));

        return full.getSubimage(x, y, w, h);
    }

    public static double devicePixelRatio(WebDriver driver) {
        Object val = ((JavascriptExecutor) driver)
                .executeScript("return window.devicePixelRatio || 1;");
        return (val instanceof Number) ? ((Number) val).doubleValue() : 1.0;
    }

    public static Mat toMat(BufferedImage bi) {
        if (bi.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            BufferedImage bgr = new BufferedImage(
                    bi.getWidth(),
                    bi.getHeight(),
                    BufferedImage.TYPE_3BYTE_BGR
            );
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

    public static Mat toGray(Mat src) {
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        return gray;
    }
}
