import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;


public class PreStepToExtractData {
    protected static WebDriver driver;
    protected static utils.ElementActions actions;
    protected static utils.PopupDataExtractor popUp;
    By fileXpath = By.xpath("//span[text()='File']");
    By exportXpath = By.xpath("//*[@data-unique-id='FileMenuExportSection']");
    By downloadCsvXpath = By.xpath("//span[text()='Download as CSV UTF-8']");
    String downloadPath = System.getProperty("user.dir") + "\\src\\test\\resources\\testdata";
    String outputCsvPath = System.getProperty("user.dir") + "\\src\\test\\resources\\testdata\\userData.csv";
    List<Map<String, String>> rows;

    @BeforeTest
    public void baseSetup() {

        if (driver == null) {   // important safety check
            // Chrome preferences
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadPath);
            prefs.put("download.prompt_for_download", false);
            prefs.put("download.directory_upgrade", true);
            prefs.put("safebrowsing.enabled", true);

            // Chrome options
            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("prefs", prefs);

            driver = new ChromeDriver(options);
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            actions = new utils.ElementActions(driver);
            popUp = new utils.PopupDataExtractor(driver);

        }
    }

    @Test
    public void testing() throws InterruptedException {

        driver.get("https://netorgft10794464-my.sharepoint.com/:x:/g/personal/vasanth_m_aifalabs_com/IQAovF-PlcWLS7nzWhF6yx9jAZGdebW2kc5RU120V6Uw2T4?e=q9NCoE");
        driver.switchTo().frame(0);
        Actions actions1 = new Actions(driver);
        actions1.click(driver.findElement(fileXpath)).build().perform();
        Thread.sleep(2000);
        actions1.click(driver.findElement(exportXpath)).build().perform();
        Thread.sleep(2000);
        actions1.click(driver.findElement(downloadCsvXpath)).build().perform();
        Thread.sleep(5000);

        rows = utils.CsvUtils.readCsv(downloadPath + "\\AllUserDataWithLinks(in).csv");
        cleanUp(downloadPath + "\\AllUserDataWithLinks(in).csv");
        for (Map<String, String> row : rows) {

            String postUrl = row.get("User LikedIn Name");
            for (String column : row.keySet()) {

                if (column.equalsIgnoreCase("User LikedIn Name") ||
                        column.equalsIgnoreCase("Validating Urls")) {
                    continue;
                }



            }
        }
        utils.CsvUtils.writeCsv(outputCsvPath, rows);

    }

    public void cleanUp(String path) {
        File folder = new File(path);
        if (folder.isFile()) {
            folder.delete();
        }
    }
}



