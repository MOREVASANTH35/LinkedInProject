package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.*;

import utils.CsvUtils;
import utils.ElementActions;

import java.time.Duration;
import java.util.*;


public class PostLikeTest {

    WebDriver driver;
    String csvPath = "src/test/resources/testdata/users.csv";
    ElementActions actions;
    By showMoreBy = By.xpath("//button[@class='artdeco-button artdeco-button--muted artdeco-button--1 artdeco-button--full artdeco-button--secondary ember-view scaffold-finite-scroll__load-button']");
    By likedUsersBy = By.xpath("//div[@class='artdeco-entity-lockup__title ember-view']");
    List<String> likedUserList;

    @BeforeClass
    public void setup() {
        driver = new ChromeDriver();
        actions = new ElementActions(driver);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10000));
        driver.get("https://www.linkedin.com/company/aifa-labs-official/posts/?feedView=all");
        driver.findElement(By.id("username")).sendKeys("7893397180");
        driver.findElement(By.id("password")).sendKeys("Morevasanth35@");
        driver.findElement(By.xpath("//*[@type='submit']")).click();
    }

    @Test
    public void updateCsvGenerically() {

        List<Map<String, String>> rows = CsvUtils.readCsv(csvPath);

        for (Map<String, String> row : rows) {

            String postUrl = row.get("PostUrl");
            driver.get(postUrl);
            actions.scrollAndClick(By.xpath("//*[@class='social-details-social-counts__social-proof-text']"));

            // 🔹 Generic total likes
            row.put("Total Liked", getTotalLikes());
            actions.clickAndWaitForMore(showMoreBy, likedUsersBy, 20);
            likedUserList = getLikedUserList();

            // 🔹 Generic handling of dynamic columns
            for (String column : row.keySet()) {

                if (column.equalsIgnoreCase("PostUrl") ||
                        column.equalsIgnoreCase("Total Liked")) {
                    continue;
                }


                // Column name is user name
                boolean liked = isUserLiked(column);
                row.put(column, liked ? "YES" : "NO");
            }
        }

        CsvUtils.writeCsv(csvPath, rows);
    }

    private String getTotalLikes() {
        // Replace with real locator
        return driver.findElement(
                By.xpath("//*[@class='social-details-reactors-tab__tablist artdeco-tablist artdeco-tablist--no-wrap ember-view']//span[2]")
        ).getText();
    }

    private List<String> getLikedUserList() {

        List<String> likedUsers = new ArrayList<>();

        try {
            List<WebElement> elements = driver.findElements(likedUsersBy);

            for (WebElement element : elements) {
                try {
                    String text = element.getText().trim();
                    if (!text.isEmpty()) {
                        likedUsers.add(text);
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Ignore stale element and continue
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to get liked user list: " + e.getMessage());
        }

        return likedUsers;
    }

    private boolean isUserLiked(String userName) {
        // Generic check
        return likedUserList.contains(userName);
    }

    public void clickShowMoreBtn() {
        while (actions.isElementDisplayed(showMoreBy)) {
            actions.scrollAndClick(driver.findElement(showMoreBy));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }


    @AfterClass
    public void tearDown() {
        driver.quit();
    }
}
