package utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PopupDataExtractor {

    WebDriver driver;
    WebDriverWait wait;

    public PopupDataExtractor(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public List<String> getNamesFromPopup(String buttonXpath, String nameXpath) {
        List<String> names = new ArrayList<>();

        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Open popup
            WebElement button = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath(buttonXpath))
            );
            js.executeScript("arguments[0].click();", button);
            Thread.sleep(2000);

            // Locate popup
            WebElement popup = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//div[@role='dialog']")
                    )
            );

            while (true) {
                // Extract visible names
                List<WebElement> elements = popup.findElements(By.xpath(nameXpath));
                for (WebElement elem : elements) {
                    String text = elem.getText().trim();
                    if (!text.isEmpty() && !names.contains(text)) {
                        names.add(text.toLowerCase());
                    }
                }

                // Click "Show more results" if present
                try {
                    WebElement showMore = popup.findElement(
                            By.xpath(buttonXpath)
                    );
                    js.executeScript("arguments[0].click();", showMore);
                    showMore.click();
                    Thread.sleep(3000);
                } catch (Exception e) {
                  //  driver.navigate().refresh();
                    break;
                }
            }

            // Close popup
            /*WebElement closeButton = popup.findElement(
                    By.xpath("(//*[name()='svg'][@role='none'])[1]")
            );
            js.executeScript("arguments[0].click();", closeButton);
            Thread.sleep(2000);*/
            driver.findElement(By.xpath("//button[@aria-label='Dismiss']")).click();


        } catch (Exception e) {
            System.out.println("Error extracting data: " + e.getMessage());
            //driver.navigate().refresh();
        }

        return names;
    }
}

