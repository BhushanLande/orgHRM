package pageObjects;
import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.core.pages.WebElementFacade;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import utils.OpenCVUtil;
import utils.ScreenshotUtil;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import utils.VisualAssert;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

public class LoginPage extends PageObject {
    Logger logger = LoggerFactory.getLogger(getClass());
    @FindBy(xpath = "//input[@name='username']")
    WebElementFacade userNameTab;

    @FindBy(xpath = "//input[@name='password']")
    WebElementFacade passwordTab;

    @FindBy(xpath = "//button[@type='submit']")
    WebElementFacade loginButton;

    @FindBy(xpath = "//input[@placeholder='Search']")
    WebElementFacade searchInputBox;

    @FindBy(xpath = "//div[@class='oxd-sidepanel-header']//img[@alt='client brand banner']")
    WebElement logoImage;

    public void enterTheUsername(String userName) throws InterruptedException {
        Thread.sleep(3000);
        waitFor(userNameTab).waitUntilClickable();
        userNameTab.sendKeys(userName);
        logger.info("Entering username: "+userName);
    }

    public LoginPage enterThePassword(String Pwd) throws InterruptedException {
        Thread.sleep(3000);
        waitFor(passwordTab).waitUntilClickable();
        passwordTab.sendKeys(Pwd);
        logger.info("Entering password: ");
        /*JavascriptExecutor js = (JavascriptExecutor) getDriver();
        js.executeScript("document.body.style.zoom='80%'");
        ScreenshotUtil.saveSnapshot(getDriver(), "pass");
        waitForPageToBeStable();*/
        return this;
    }
    public LoginPage clickOnTheLoginButton() throws InterruptedException {
        Thread.sleep(3000);
        waitFor(loginButton).waitUntilClickable();
        clickOn(loginButton);
        logger.info("Clicking login button");
        waitForPageToBeStable();
        return this;
    }

    public void enterTheSearchOptions(String option) throws InterruptedException {
        Thread.sleep(3000);
        waitFor(searchInputBox).waitUntilClickable();
        searchInputBox.sendKeys(option);
        logger.info("Entering options: "+option);
    }

    public void clickDashboardPageUsingImage() throws Exception {
        waitFor(searchInputBox).waitUntilClickable();
        Thread.sleep(5000);
        String screenshot =ScreenshotUtil.captureScreenshot(getDriver(), "landing_screen");

        String template =
                "src/main/resources/images/landing_page.png";
        Assert.assertTrue(OpenCVUtil.clickUsingImage(getDriver(), screenshot, template), "Dashboard page not found using image recognition.");
    }

    public void validateWebElement(WebElement element,String imagePath, Double score) throws Exception {
        BufferedImage headerImg = ScreenshotUtil.elementScreenshot(getDriver(), element);
        Mat headerMat = VisualAssert.toMat(headerImg);
        Mat ctaNeedle = Imgcodecs.imread(imagePath);
        VisualAssert.TemplateResult tr = VisualAssert.matchTemplate(headerMat, ctaNeedle);
        logger.info("Validating element against template: " + imagePath + " | Match score: " + tr.score);
        Assert.assertTrue(tr.score >= score, "CTA not found with sufficient confidence: " + tr.score);
    }

    public void validateLogoAndHeader() throws Exception {
        WebElement header = getDriver().findElement(By.xpath("//h6[text()='Dashboard']"));
        validateWebElement(header, "src/main/resources/WebElements/DashboardLogo.png", 0.85);
        validateWebElement(logoImage, "src/main/resources/WebElements/Logo.png", 0.95);
        WebElement timePanel = getDriver().findElement(By.xpath("(//div[@class='oxd-layout-context']//div[contains(@class,'orangehrm-dashboard-widget')])[1]/div"));
        validateWebElement(timePanel, "src/main/resources/WebElements/profilePanel.png", 0.9);
    }

    public void waitForPageToBeStable() {
        waitForCondition().until(
                driver -> ((JavascriptExecutor) driver)
                        .executeScript("return document.readyState")
                        .equals("complete")
        );

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setPixels(int width, int height, double deviceScaleFactor) {
        WebDriver driver = getDriver();

        if (!(driver instanceof ChromeDriver)) {
            logger.warn("setPixels(): Non-Chromium driver detected ({}). Skipping CDP override.", driver.getClass().getSimpleName());
            return;
        }

        try {
            ChromeDriver chrome = (ChromeDriver) driver;

            // Create a DevTools session
            DevTools devTools = ((HasDevTools) chrome).getDevTools();
            devTools.createSession();

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("width", width);
            metrics.put("height", height);
            metrics.put("deviceScaleFactor", deviceScaleFactor);
            metrics.put("mobile", false);

            chrome.executeCdpCommand("Emulation.setDeviceMetricsOverride", metrics);
            logger.info("Viewport locked via CDP: {}x{}, dpr={}", width, height, deviceScaleFactor);
        } catch (Exception e) {
            logger.error("Failed to set viewport pixels via CDP: {}", e.getMessage(), e);
        }
    }

    public void setSizeAndCenterPrimary(int width, int height) {
        getDriver().manage().window().setSize(new Dimension(width, height));
        java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;
        getDriver().manage().window().setPosition(new Point(x, y));
    }

    public void setSizeAndCenterWorkArea( int width, int height) {
        getDriver().manage().window().setSize(new Dimension(width, height));
        java.awt.Rectangle work = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int x = work.x + (work.width - width) / 2;
        int y = work.y + (work.height - height) / 2;
        getDriver().manage().window().setPosition(new Point(x, y));
    }

    public void setSizeAndCenterOnDisplay(int width, int height, int displayIndex) {
        getDriver().manage().window().setSize(new Dimension(width, height));
        java.awt.GraphicsDevice[] devices = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (displayIndex < 0 || displayIndex >= devices.length) {
            throw new IllegalArgumentException("Invalid display index " + displayIndex);
        }
        java.awt.Rectangle b = devices[displayIndex].getDefaultConfiguration().getBounds();
        int x = b.x + (b.width - width) / 2;
        int y = b.y + (b.height - height) / 2;
        getDriver().manage().window().setPosition(new Point(x, y));
    }

    public enum CenterMode { PRIMARY, WORKAREA, DISPLAY }

    public void setSizeAndCenter( int targetW, int targetH, CenterMode mode, double displayIndex) throws InterruptedException {
        try {
            getDriver().manage().window().setPosition(new Point(0, 0));
        }
        catch (Exception ignored) {}
        sleep(100);

        getDriver().manage().window().setSize(new Dimension(targetW, targetH));

        java.awt.Rectangle bounds;
        switch (mode) {
            case WORKAREA:
                bounds = java.awt.GraphicsEnvironment
                        .getLocalGraphicsEnvironment().getMaximumWindowBounds();
                break;
            case DISPLAY:
                java.awt.GraphicsDevice[] devices = java.awt.GraphicsEnvironment
                        .getLocalGraphicsEnvironment().getScreenDevices();
                if (displayIndex < 0 || displayIndex >= devices.length) {
                    throw new IllegalArgumentException("Invalid display index " + displayIndex +
                            " (available: 0.." + (devices.length - 1) + ")");
                }
                bounds = devices[(int) displayIndex].getDefaultConfiguration().getBounds();
                break;
            case PRIMARY:
            default:
                java.awt.Dimension scr = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                bounds = new java.awt.Rectangle(0, 0, scr.width, scr.height);
        }

        // 3) Compute ideal center position
        int idealX = bounds.x + (bounds.width - targetW) / 2;
        int idealY = bounds.y + (bounds.height - targetH) / 2;

        // 4) Move, then verify and compensate if actual size differs
        getDriver().manage().window().setPosition(new Point(idealX, idealY));
        sleep(100);

        // 5) Re-read actual window size and adjust position to keep it centered
        Dimension actual = getDriver().manage().window().getSize();
        int dx = (targetW - actual.width) / 2;
        int dy = (targetH - actual.height) / 2;

        if (dx != 0 || dy != 0) {
            getDriver().manage().window().setPosition(new Point(idealX + dx, idealY + dy));
        }
        for (int i = 0; i < 3; i++) {
            sleep(100);
            Dimension s = getDriver().manage().window().getSize();
            if (Math.abs(s.width - targetW) <= 1 && Math.abs(s.height - targetH) <= 1) break;
            getDriver().manage().window().setSize(new Dimension(targetW, targetH));
        }
    }

}