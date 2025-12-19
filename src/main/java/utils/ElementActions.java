package utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;

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
    public void clickAndWaitForMore(By buttonBy, int maxAttempts) {




         //   scrollUntilItDisappeared(buttonBy,maxAttempts);

           /* wait.until(driver ->
                    driver.findElements(contentBy).size() > currentSize
            );
*/

    }
    public void scrollUntilItDisappears(By locator, int maxScrolls) {

        JavascriptExecutor js = (JavascriptExecutor) driver;

        for (int i = 0; i < maxScrolls; i++) {

            // If element is gone → stop
            if (driver.findElements(locator).isEmpty()) {
                System.out.println("Element disappeared after " + i + " scrolls");
                break;
            }

            try {
                WebElement element = driver.findElement(locator);

                js.executeScript(
                        "arguments[0].scrollIntoView({block:'end'});", element);

                // Small wait for DOM update
                Thread.sleep(800);

            } catch (StaleElementReferenceException e) {
                // DOM refreshed → re-check in next iteration
            } catch (Exception e) {
                break;
            }
        }


    }
    public void scrollUntilItDisappears1(By locator, int maxScrolls) {

        JavascriptExecutor js = (JavascriptExecutor) driver;

        for (int i = 0; i < maxScrolls; i++) {

            List<WebElement> elements = driver.findElements(locator);

            // Element gone OR not visible → STOP
            if (elements.isEmpty() || !elements.get(0).isDisplayed()) {
                System.out.println("Element disappeared at scroll: " + i);
                break;
            }

            js.executeScript(
                    "arguments[0].scrollIntoView({block:'end'});", elements);
            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) {}
        }
    }

    public void clickUntilGone(By locator, int maxClicks) {

        JavascriptExecutor js = (JavascriptExecutor) driver;

        for (int i = 0; i < maxClicks; i++) {

            List<WebElement> elements = driver.findElements(locator);

            if (elements.isEmpty() || !elements.get(0).isDisplayed()) {
                break;
            }

            js.executeScript("arguments[0].click();", elements.get(0));

            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}
        }
    }

    public void customSleep(int sec)  {
        try {
            long wait=sec*1000;
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public void scrollNTimes(int number){
        JavascriptExecutor js = (JavascriptExecutor) driver;

        for (int i = 0; i < number; i++) {
            js.executeScript("window.scrollBy(0,1000);");
            customSleep(1);
        }
    }
    public void scrollToElement(By locator) {
        WebElement element = driver.findElement(locator);
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

}
