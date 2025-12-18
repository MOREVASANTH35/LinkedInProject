package tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import utils.ConfigReader;
import utils.ElementActions;
import utils.PasswordDecryptor;
import utils.PopupDataExtractor;

import java.time.Duration;

public class BaseTest {

    protected WebDriver driver;
    protected ElementActions actions;
    protected PopupDataExtractor popUp;

    @BeforeClass
    public void baseSetup() {

        driver = new ChromeDriver();
        actions = new ElementActions(driver);
        popUp = new PopupDataExtractor(driver);

        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        loginToLinkedIn();
    }

    protected void loginToLinkedIn() {

        driver.get(ConfigReader.get("app.url"));

        driver.findElement(By.id("username"))
                .sendKeys(ConfigReader.get("username"));

        String decryptedPassword =
                PasswordDecryptor.decrypt(ConfigReader.get("password"));

        driver.findElement(By.id("password"))
                .sendKeys(decryptedPassword);

        driver.findElement(By.xpath("//*[@type='submit']")).click();
    }

    @AfterClass
    public void baseTearDown() {
         driver.quit();
    }
}
