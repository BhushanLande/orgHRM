package resources.DriverProvider;

import io.github.bonigarcia.wdm.WebDriverManager;
import net.thucydides.core.webdriver.DriverSource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class CustomDriverSource implements DriverSource {

    static {
        System.setProperty("wdm.edgeDriverUrl", "https://msedgedriver.microsoft.com/");
        System.setProperty("webdriver.firefox.bin", "C://Program Files//Mozilla Firefox//firefox.exe");
        System.setProperty("webdriver.http.factory", "jdk-http-client");
    }

    @Override
    public WebDriver newDriver() {

        String browser = System.getProperty(
                "browser",
        System.getenv().getOrDefault("BROWSER", "chrome")
        ).toLowerCase();

        System.out.println("[CustomDriverSource] browser = " + browser);

        switch (browser) {

            case "chrome": {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");

            return new ChromeDriver(options);
        }

            case "firefox": {
            WebDriverManager.firefoxdriver().setup();
            FirefoxOptions options = new FirefoxOptions();
            options.addArguments("--remote-allow-origins=");
            options.setBinary("C://Program Files//Mozilla Firefox//firefox.exe");

            return new FirefoxDriver(options);
        }

            case "edge":
            default: {
            WebDriverManager.edgedriver().setup();

            EdgeOptions options = new EdgeOptions();
            options.addArguments("--start-maximized");
            options.addArguments("--remote-allow-origins=");

            return new EdgeDriver(options);
        }
        }
    }

    @Override
    public boolean takesScreenshots() {
        return true;
    }
}
