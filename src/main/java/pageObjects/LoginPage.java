package pageObjects;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.core.pages.WebElementFacade;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import org.openqa.selenium.support.FindBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.OcrUtils;
import utils.OpenCVUtil;
import utils.ScreenshotUtil;
import utils.VisualAssert;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.testng.Assert.*;

public class LoginPage extends PageObject {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @FindBy(xpath = "//input[@name='username']")
    private WebElementFacade userNameTab;

    @FindBy(xpath = "//input[@name='password']")
    private WebElementFacade passwordTab;

    @FindBy(xpath = "//button[@type='submit']")
    private WebElementFacade loginButton;

    @FindBy(xpath = "//input[@placeholder='Search']")
    private WebElementFacade searchInputBox;
    @FindBy(xpath = "//h6[text()='Dashboard']")
    private WebElementFacade dashboardHeader;

    private static final double SSIM_THRESHOLD = 0.985;
    private static final double THEME_THRESHOLD = 0.95;
    private static final double DIFF_THRESHOLD = 1.5;
    private static final double TEMPLATE_THRESHOLD = 0.92;
    private static final double WCAG_CONTRAST_THRESHOLD = 4.5;

    public void enterTheUsername(String userName) {
        userNameTab.waitUntilClickable();
        userNameTab.clear();
        userNameTab.sendKeys(userName);
        logger.info("Entered username: {}", userName);
    }

    public LoginPage enterThePassword(String password) {
        passwordTab.waitUntilClickable();
        passwordTab.clear();
        passwordTab.sendKeys(password);
        logger.info("Entered password");
        return this;
    }

    public LoginPage clickOnTheLoginButton() throws InterruptedException {
        loginButton.waitUntilClickable();
        clickOn(loginButton);
        logger.info("Clicked login button");
        Thread.sleep(3000);
        return this;
    }

    public void enterTheSearchOptions(String option) throws InterruptedException {
        searchInputBox.waitUntilClickable();
        searchInputBox.clear();
        searchInputBox.sendKeys(option);
        logger.info("Entered search option: {}", option);
        Thread.sleep(3000);
    }

    public void clickLoginPageUsingImage() {
        waitFor(5).seconds();
        userNameTab.waitUntilVisible();

        String screenshot = ScreenshotUtil.captureScreenshot(getDriver(), "login_page");
        String template = "src/main/resources/images/login_page.png";
        ScreenshotUtil.saveSnapshot(getDriver(), "login_page");
        assertTrue(
                OpenCVUtil.clickUsingImage(getDriver(), screenshot, template),
                "Login page not found using image recognition."
        );
    }

    public void clickDashboardPageUsingImage() {
        searchInputBox.waitUntilVisible();

        String screenshot = ScreenshotUtil.captureScreenshot(getDriver(), "landing_screen");
        String template = "src/main/resources/images/dashboard_page.png";
        assertTrue(
                OpenCVUtil.clickUsingImage(getDriver(), screenshot, template),
                "Dashboard page not found using image recognition."
        );
    }

    public void visualRegressionHome() throws Exception {

        // 1️⃣ Capture full-page screenshot
        BufferedImage actualImg = ScreenshotUtil.fullPageScreenshot(getDriver());
        logger.info("Full-page screenshot size: {}x{}", actualImg.getWidth(), actualImg.getHeight());

        Mat actual = ScreenshotUtil.toMat(actualImg);

        // 2️⃣ Load baseline image for full page
        Mat baseline = Imgcodecs.imread("src/main/resources/images/dashboard_page.png");
        assertFalse(baseline.empty(), "Baseline image missing.");

        // ✅ Full-page SSIM
        double ssim = VisualAssert.ssim(actual, baseline);
        logger.info("Full-page SSIM Score: {}", ssim);
//        assertTrue(ssim >= SSIM_THRESHOLD, "Full-page SSIM below threshold: " + ssim);

        // 3️⃣ Template Matching on smaller element (dashboard header)
        BufferedImage headerImg = ScreenshotUtil.elementScreenshot(getDriver(), dashboardHeader);
        Mat headerMat = ScreenshotUtil.toMat(headerImg);

        Mat headerTemplate = Imgcodecs.imread("src/main/resources/images/dashboard_header.png");
        assertFalse(headerTemplate.empty(), "Dashboard header template missing.");

        // Resize template if larger than element
        if (headerTemplate.width() > headerMat.width() || headerTemplate.height() > headerMat.height()) {
            headerTemplate = OpenCVUtil.resizeMat(headerTemplate, headerMat.width(), headerMat.height());
            logger.info("Resized template to fit header element: {}x{}", headerMat.width(), headerMat.height());
        }

        VisualAssert.TemplateResult tr = VisualAssert.matchTemplate(headerMat, headerTemplate);
        logger.info("Header Template Match Score: {}", tr.score);
//        assertTrue(tr.score >= TEMPLATE_THRESHOLD, "Template match confidence too low: " + tr.score);

        // 4️⃣ Theme correlation
        double themeCorr = VisualAssert.hsvCorrelation(actual, baseline);
        logger.info("Theme correlation: {}", themeCorr);
//        assertTrue(themeCorr >= THEME_THRESHOLD, "Theme histogram diverged: " + themeCorr);

        // 6️⃣ WCAG Contrast
        Color bg = new Color(headerImg.getRGB(headerImg.getWidth() / 2, headerImg.getHeight() / 2));
        Color fg = Color.ORANGE;
        double contrast = VisualAssert.wcagContrastRatio(fg, bg);
        logger.info("WCAG Contrast ratio: {}", contrast);
//        assertTrue(contrast >= WCAG_CONTRAST_THRESHOLD, "Contrast ratio below WCAG AA: " + contrast);

        // 7️⃣ Visual Diff with Mask
        Mat mask = Mat.ones(actual.size(), CvType.CV_8UC1);
        VisualAssert.DiffResult dr = VisualAssert.diffWithMask(actual, baseline, mask);
        logger.info("Changed %: {}", dr.changedPct);
        Imgcodecs.imwrite("artifacts/home_diff_overlay.png", dr.diffBgr);
//        assertTrue(dr.changedPct < DIFF_THRESHOLD, "Too many pixel changes detected: " + dr.changedPct);
    }

}
