import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;
import utils.CsvUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


public class PostLikeTest extends BaseTest {

    String csvPath = "src/test/resources/testdata/userData.csv";
    String outputCsvPath = "src/test/resources/testOutput/OutputUserLikes.csv";

    By showMoreBy = By.xpath(
            "//button[@class='artdeco-button artdeco-button--muted artdeco-button--1 " +
                    "artdeco-button--full artdeco-button--secondary ember-view scaffold-finite-scroll__load-button']"
    );

    By likedUsersBy = By.xpath(
            "//div[@class='artdeco-entity-lockup__title ember-view'] | " +
                    "//div[@class='artdeco-entity-lockup__title ember-view']//span[1]"
    );

    List<String> likedUserList;

    @Test(groups = {"like", "smoke"})
    public void updateCsvGenerically() {
        CsvUtils.copyCsvFile(csvPath,outputCsvPath);
        List<Map<String, String>> rows = CsvUtils.readCsv(csvPath);

        // 🔹 IST timestamp formatter
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        for (Map<String, String> row : rows) {

            String postUrl = row.get("PostUrl");
            driver.navigate().to(postUrl);

            actions.scrollAndClick(
                    By.xpath("//*[@class='social-details-social-counts__social-proof-text']")
            );

            actions.customSleep(5);

            row.put("Total", getTotalLikes());

            actions.scrollUntilItDisappears(showMoreBy, 20);

            likedUserList = getLikedUserList();

            int yesCount = 0;
            int totalUsers = 0;

            for (String column : row.keySet()) {

                if (column.equalsIgnoreCase("PostUrl") ||
                        column.equalsIgnoreCase("Total") ||
                        column.equalsIgnoreCase("Yes %") ||
                        column.equalsIgnoreCase("Executed At (IST)")) {
                    continue;
                }

                totalUsers++;

                boolean liked = isUserLiked(column);
                row.put(column, liked ? "YES" : "NO");

                if (liked) {
                    yesCount++;
                }
            }

            // 🔹 Calculate Yes %
            double likedPercentage =
                    totalUsers == 0 ? 0 : (yesCount * 100.0) / totalUsers;

            row.put("Yes %", String.format("%.2f%%", likedPercentage));

            // 🔹 Add Execution Timestamp (IST) as LAST column
            String istTime = ZonedDateTime
                    .now(ZoneId.of("Asia/Kolkata"))
                    .format(formatter);

            row.put("Executed At (IST)", istTime);
        }

        CsvUtils.writeCsv(outputCsvPath, rows);
    }


    private String getTotalLikes() {
        return driver.findElement(
                By.xpath("//*[@class='social-details-reactors-tab__tablist artdeco-tablist artdeco-tablist--no-wrap ember-view']//span[2]")
        ).getText();
    }

    private List<String> getLikedUserList() {

        List<String> likedUsers = new LinkedList<>();

        try {
            List<WebElement> elements = driver.findElements(likedUsersBy);

            for (WebElement element : elements) {
                try {
                    String text = element.getText().trim();
                    if (!text.isEmpty()) {
                        likedUsers.add(text.toLowerCase());
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to get liked user list: " + e.getMessage());
        }

        driver.findElement(By.xpath("//button[@aria-label='Dismiss']")).click();
        return likedUsers;
    }

    private boolean isUserLiked(String userName) {
        return likedUserList.contains(userName.toLowerCase());
    }
}
