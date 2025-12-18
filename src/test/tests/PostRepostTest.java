package tests;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;
import utils.CsvUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class PostRepostTest extends BaseTest {

    String csvPath = "src/test/resources/testdata/usersRepost.csv";

    By showMoreBy = By.xpath(
            "//button[@class='artdeco-button artdeco-button--muted artdeco-button--1 " +
                    "artdeco-button--full artdeco-button--secondary ember-view scaffold-finite-scroll__load-button']"
    );
    By repostUsersBy = By.xpath("//*[@class='update-components-header__text-view']//a");
    By repostsBtn = By.xpath("(//li[@class='display-flex flex-grow-1 max-full-width']//button)[2]");
    List<String> repostsUserList;

    @Test(groups = {"like", "repost"})
    public void updateCsvGenerically() {

        List<Map<String, String>> rows = CsvUtils.readCsv(csvPath);

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        for (Map<String, String> row : rows) {

            String postUrl = row.get("PostUrl");
            driver.navigate().to(postUrl);
            row.put("Total Commented", getTotalRePosts());
            actions.scrollAndClick(repostsBtn);

            actions.customSleep(5);


            actions.scrollUntilItDisappears(showMoreBy,10);

            repostsUserList = getRepostUserList();

            int yesCount = 0;
            int totalUsers = 0;

            for (String column : row.keySet()) {

                if (column.equalsIgnoreCase("PostUrl") ||
                        column.equalsIgnoreCase("Total Commented") ||
                        column.equalsIgnoreCase("Liked %") ||
                        column.equalsIgnoreCase("Executed At (IST)")) {
                    continue;
                }

                totalUsers++;

                boolean commented = isUserReposted(column);
                row.put(column, commented ? "YES" : "NO");

                if (commented) {
                    yesCount++;
                }
            }

            // 🔹 Calculate Commented %
            double commentedPercentage =
                    totalUsers == 0 ? 0 : (yesCount * 100.0) / totalUsers;

            row.put("Liked %", String.format("%.2f%%", commentedPercentage));

            // 🔹 Add Execution Timestamp (IST) as LAST column
            String istTime = ZonedDateTime
                    .now(ZoneId.of("Asia/Kolkata"))
                    .format(formatter);

            row.put("Executed At (IST)", istTime);
        }

        CsvUtils.writeCsv(csvPath, rows);
    }


    private String getTotalRePosts() {
        return driver.findElement(repostsBtn).getText();
    }

    private List<String> getRepostUserList() {

        List<String> repostedUsers = new LinkedList<>();

        try {
            List<WebElement> elements = driver.findElements(repostUsersBy);

            for (WebElement element : elements) {
                try {
                    String text = element.getText().trim();
                    if (!text.isEmpty()) {
                        repostedUsers.add(text.toLowerCase());
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to get Commented user list: " + e.getMessage());
        }

        driver.findElement(By.xpath("//button[@aria-label='Dismiss']")).click();
        return repostedUsers;
    }

    private boolean isUserReposted(String userName) {
        return repostsUserList.contains(userName.toLowerCase());
    }
}
