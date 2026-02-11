package pageObjects;
import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.core.pages.WebElementFacade;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginPage extends PageObject {
    Logger logger = LoggerFactory.getLogger(getClass());
    @FindBy(name = "//input[@name='username']")
    WebElementFacade userNameTab;

    @FindBy(xpath = "//input[@name='password']")
    WebElementFacade passwordTab;

    @FindBy(xpath = "//button[@type='login']")
    WebElementFacade loginButton;

    @FindBy(xpath = "//input[@placeholder='Search']")
    WebElementFacade searchInputBox;

    public void openSite() {
        String url = "https://opensource-demo.orangehrmlive.com/web/index.php/auth/login";
        getDriver().get(url);
        logger.info("Opening URL: "+url);
    }
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
        return this;
    }
    public LoginPage clickOnTheLoginButton() throws InterruptedException {
        Thread.sleep(3000);
        waitFor(loginButton).waitUntilClickable();
        clickOn(loginButton);
        logger.info("Clicking login button");
        return this;
    }

    public void enterTheSearchOptions(String option) throws InterruptedException {
        Thread.sleep(3000);
        waitFor(searchInputBox).waitUntilClickable();
        searchInputBox.sendKeys(option);
        logger.info("Entering options: "+option);
    }


}

