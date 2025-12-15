package utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;

public class ElementActions {

    private WebDriver driver;
    private WebDriverWait wait;

    public ElementActions(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(25));
    }

    /* ================= SCROLL INTO VIEW AND CLICK ================= */

    public void scrollAndClick(By locator) {
        try {
            WebElement element = wait.until(
                    ExpectedConditions.presenceOfElementLocated(locator)
            );

            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].scrollIntoView({block:'center'});", element);
            Thread.sleep(1000);

            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();

        } catch (Exception e) {
            throw new RuntimeException("Failed to scroll and click: " + locator, e);
        }
    }

    /* ================= SCROLL AND CLICK (WEBELEMENT) ================= */

    public void scrollAndClick(WebElement element) {
        try {
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].scrollIntoView({block:'center'});", element);

            wait.until(ExpectedConditions.elementToBeClickable(element)).click();

        } catch (Exception e) {
            System.out.println("Failed to scroll and click element"+e);
        }
    }
    public boolean isElementDisplayed(By locator) {
        try {
            WebElement element = wait.until(
                    ExpectedConditions.presenceOfElementLocated(locator)
            );
            return element.isDisplayed();
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        } catch (StaleElementReferenceException e) {
            // Retry once for stale element
            try {
                WebElement element = driver.findElement(locator);
                return element.isDisplayed();
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /* ========== CHECK IF ELEMENT IS DISPLAYED (WEBELEMENT) ========== */

    public boolean isElementDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (StaleElementReferenceException | NoSuchElementException e) {
            return false;
        }
    }
    public void clickUntilNotDisplayed(By locator, int maxAttempts) {

        int attempt = 0;

        while (attempt < maxAttempts && isElementDisplayed(locator)) {
            try {
                WebElement element = wait.until(
                        ExpectedConditions.elementToBeClickable(locator)
                );

                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].scrollIntoView({block:'center'});", element);

                element.click();

                // wait for DOM update
                wait.until(ExpectedConditions.stalenessOf(element));

            } catch (TimeoutException | StaleElementReferenceException ignored) {
            }

            attempt++;
        }
    }
    public void clickAndWaitForMore(By buttonBy, By contentBy, int maxAttempts) {

        int attempt = 0;



            scrollUntilItDisappeared(buttonBy);

           /* wait.until(driver ->
                    driver.findElements(contentBy).size() > currentSize
            );
*/
            attempt++;

    }
    public void scrollUntilItDisappeared(By locator) {

        int maxScrolls = 20;
        int scrollCount = 0;

        while (scrollCount < maxScrolls) {

            // If element is no longer visible → stop
            if (!isElementDisplayed(locator)) {
                break;
            }

            try {
                WebElement element = driver.findElement(locator);

                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].scrollIntoView({block:'end'});", element);

                // Wait for DOM to update / new content load
                wait.until(ExpectedConditions.stalenessOf(element));

            } catch (StaleElementReferenceException | NoSuchElementException e) {
                // Element disappeared
                break;
            } catch (TimeoutException ignored) {
                // Element still present, continue scrolling
            }

            scrollCount++;
        }
    }


}
