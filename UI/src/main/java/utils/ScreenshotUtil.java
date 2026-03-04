package utils;
import org.openqa.selenium.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.Graphics2D;

import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;

public class ScreenshotUtil {

    public static String captureScreenshot(WebDriver driver, String fileName) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String path = "screenshots/" + fileName + ".png";
//            FileUtils.copyFile(src, new File(path));
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

    private static final String FOLDER = "src/main/resources/ElementScreenshots";

    public static BufferedImage viewportScreenshot(WebDriver driver) throws Exception {
        byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        // Avoid disk cache for ImageIO
        ImageIO.setUseCache(false);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            return ImageIO.read(bais);
        }
    }

    public static double devicePixelRatio(WebDriver driver) {
        Object val = ((JavascriptExecutor) driver).executeScript("return window.devicePixelRatio || 1;");
        return (val instanceof Number) ? ((Number) val).doubleValue() : 1.0;
    }

    private static long scrollX(WebDriver driver) {
        Object val = ((JavascriptExecutor) driver).executeScript(
                "return window.pageXOffset || document.documentElement.scrollLeft || 0;");
        return (val instanceof Number) ? ((Number) val).longValue() : 0L;
    }

    private static long scrollY(WebDriver driver) {
        Object val = ((JavascriptExecutor) driver).executeScript(
                "return window.pageYOffset || document.documentElement.scrollTop || 0;");
        return (val instanceof Number) ? ((Number) val).longValue() : 0L;
    }

    public static BufferedImage elementScreenshot(WebDriver driver, WebElement el) throws Exception {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        BufferedImage full = viewportScreenshot(driver);
        double dpr = devicePixelRatio(driver);

        org.openqa.selenium.Point loc = el.getLocation();
        org.openqa.selenium.Dimension size = el.getSize();

        long sx = scrollX(driver);
        long sy = scrollY(driver);

        int x = (int) Math.round((loc.getX() - sx) * dpr);
        int y = (int) Math.round((loc.getY() - sy) * dpr);
        int w = (int) Math.round(size.getWidth() * dpr);
        int h = (int) Math.round(size.getHeight() * dpr);

        // Clamp to image
        x = Math.max(0, Math.min(x, full.getWidth() - 1));
        y = Math.max(0, Math.min(y, full.getHeight() - 1));
        w = Math.max(1, Math.min(w, full.getWidth() - x));
        h = Math.max(1, Math.min(h, full.getHeight() - y));

        // IMPORTANT: create a deep copy so we can free the big "full" image
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        try {
            g.drawImage(full, 0, 0, w, h, x, y, x + w, y + h, null);
        } finally {
            g.dispose();
            full.flush(); // free memory
        }
        //saveElementScreenshotDirect(el, withTimestamp("element_" + el.getTagName()));
        return copy;
    }

    public static String withTimestamp(String base) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        return base.replaceAll("[^a-zA-Z0-9-\\.]", "") + "_" + ts;
    }

    public static Path saveElementScreenshotDirect(WebElement el, String fileNameNoExt) throws Exception {
        Path dir = Paths.get(FOLDER);
        if (!Files.exists(dir)) Files.createDirectories(dir);

        File src = el.getScreenshotAs(OutputType.FILE);
        Path target = dir.resolve(fileNameNoExt + ".png");
        Files.copy(src.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }
}