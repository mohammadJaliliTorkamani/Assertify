package org.example;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.annotation.Nonnull;

/**
 * a singleton class for configuration of the seleniumDriver scrapper
 */
public class SeleniumConfig implements AutoCloseable {
    /**
     * the local path of the chrome diver
     */
    final static String CHROME_DRIVER_PATH = "G:\\Software\\chromedriver.exe";
    private static SeleniumConfig config = null;
    private final WebDriver driver;

    private SeleniumConfig(@Nonnull WebDriver driver) {
        this.driver = driver;
    }

    /**
     * singleton seleniumConfig instance creator
     *
     * @return singleton instance of Selenium configuration
     */
    public static SeleniumConfig getInstance() {
        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
        System.setProperty("webdriver.chrome.silentOutput", "true");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        if (config == null)
            config = new SeleniumConfig(new ChromeDriver(options));
        return config;
    }

    public WebDriver getDriver() {
        return driver;
    }

    /**
     * closes the chromeDriver resource after each use
     */
    @Override
    public void close() {
        this.driver.quit();
    }
}
