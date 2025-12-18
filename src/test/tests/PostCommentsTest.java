package tests;

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


public class PostCommentsTest extends BaseTest {

    String csvPath = "src/test/resources/testdata/usersComments.csv";

    By showMoreBy = By.xpath("//button[@class='artdeco-button artdeco-button--muted artdeco-button--1 artdeco-button--full artdeco-button--secondary ember-view scaffold-finite-scroll__load-button']");
    By commentsUsersBy = By.xpath("//span[@class='comments-comment-meta__description-title']" );

    List<String> commentsUserList;

    @Test(groups = {"comments", "smoke"})
    public void updateCsvGenerically() {

        List<Map<String, String>> rows = CsvUtils.readCsv(csvPath);

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        for (Map<String, String> row : rows) {

            String postUrl = row.get("PostUrl");
            driver.navigate().to(postUrl);

            actions.scrollAndClick(
                    By.xpath("(//li[@class='display-flex flex-grow-1 max-full-width']//button)[1]")
            );

            actions.customSleep(5);

            row.put("Total Commented", getTotalComments());

            actions.scrollNTimes(10);

            commentsUserList = getCommentsUserList();

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

                boolean commented = isUserCommented(column);
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



    private String getTotalComments() {
        return driver.findElement(
                By.xpath("(//li[@class='display-flex flex-grow-1 max-full-width']//button)[1]")
        ).getText();
    }

    private List<String> getCommentsUserList() {

        List<String> commentedUsers = new LinkedList<>();

        try {
            List<WebElement> elements = driver.findElements(commentsUsersBy);

            for (WebElement element : elements) {
                try {
                    String text = element.getText().trim();
                    if (!text.isEmpty()) {
                        commentedUsers.add(text.toLowerCase());
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to get Commented user list: " + e.getMessage());
        }

       // driver.findElement(By.xpath("//button[@aria-label='Dismiss']")).click();
        return commentedUsers;
    }

    private boolean isUserCommented(String userName) {
        return commentsUserList.contains(userName.toLowerCase());
    }
}
