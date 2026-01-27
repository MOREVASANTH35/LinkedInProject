import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.time.Duration;

public class BaseTest {

    protected static WebDriver driver;
    protected static utils.ElementActions actions;
    protected static utils.PopupDataExtractor popUp;

    @BeforeSuite
    public void baseSetup() {

        if (driver == null) {   // important safety check
            driver = new ChromeDriver();
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            actions = new utils.ElementActions(driver);
            popUp = new utils.PopupDataExtractor(driver);

            loginToLinkedIn();
        }
    }

    protected void loginToLinkedIn() {

        driver.get(utils.ConfigReader.get("app.url"));

        driver.findElement(By.id("username"))
                .sendKeys(utils.ConfigReader.get("username"));

        String decryptedPassword =
                utils.PasswordDecryptor.decrypt(utils.ConfigReader.get("password"));

        driver.findElement(By.id("password"))
                .sendKeys(decryptedPassword);

        driver.findElement(By.xpath("//*[@type='submit']")).click();
    }

    @AfterSuite
    public void baseTearDown() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
    public static void customSleep(int seconds){
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
