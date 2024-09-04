package scraping.main;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


public class ScrapingUpdateService {
    private static WebDriver driver;
    private String link;
    private List<Flight> flightList = new ArrayList<Flight>();
    private List<UpdateFlightQuery> updateFlightQueryList = new ArrayList<>();
    private List<UpdateFlightQuery> returnedList = new ArrayList<>();

    public ScrapingUpdateService(List<Flight> inputtedFlightList) {
        // uncomment for completely new startup only
        //WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--headless");
        options.addArguments("--disable-gpu"); // Applicable for Windows environment to avoid crash
        options.addArguments("--no-sandbox"); // Bypass OS security model
        options.addArguments("--disable-dev-shm-usage"); // Overcome limited resource problems
        options.addArguments("--window-size=1920x1080"); // Set window size to avoid element not interactable issues
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);

        Flight listFlightParameters = inputtedFlightList.get(0); // for getting locations and dates of flights I'm looking for
        String flightStart = listFlightParameters.getFlightStart();
        String flightDestination = listFlightParameters.getFlightDestination();
        String leaveDate = listFlightParameters.getLeaveDate();
        String returnDay = listFlightParameters.getReturnDay();
        this.link = "https://www.google.com/travel/flights?q=flights+from+" + flightStart + "+to+" + flightDestination + "+on+" + leaveDate + "+through+" + returnDay;
        this.flightList = inputtedFlightList;

        System.out.println(link);
        driver.get(link);
    }

    public List<UpdateFlightQuery> checkNewPriceAndLink() throws InterruptedException {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            int scrollAmount = 0;
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            js.executeScript("window.scrollBy(0,1000)", "");
            WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".zISZ5c.QB2Jof")));
            button.click();
            Thread.sleep(2000);
            
            List<WebElement> flights = retryFindElements(".pIav2d", 20, null);

            js.executeScript("window.scrollBy(0,-10000)", "");
            Thread.sleep(1000);
            for(int i = 0; i < flights.size(); i++) {
                UpdateFlightQuery updateFlightQuery = new UpdateFlightQuery();
                updateFlightQuery = getPriceData(flights.get(i), updateFlightQuery, i);
                updateFlightQueryList.add(updateFlightQuery);
            }

            // mapping original flight list
            Map<String, Flight> originalFlightMap = flightList.stream()
                    .collect(Collectors.toMap(Flight::getFlightImpactLink, entry -> entry));

            List<Pair<UpdateFlightQuery, Flight>> differingPairs = updateFlightQueryList.stream()
            .filter(entryA -> {
                Flight entryB = originalFlightMap.get(entryA.getFlightImpactLink());
                return entryB != null && !(entryA.getPrice() == (entryB.getPrice()));
            })
            .map(entryA -> Pair.of(entryA, originalFlightMap.get(entryA.getFlightImpactLink())))
            .collect(Collectors.toList());

            for(Pair<UpdateFlightQuery, Flight> pair : differingPairs) {
                UpdateFlightQuery updatedFlight = pair.getLeft();
                Flight originalFlight = pair.getRight();
                updatedFlight.setId(originalFlight.getId());

                // getting new link
                js.executeScript("window.scrollBy(0,10000)", "");
                button = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".zISZ5c.QB2Jof")));
                Thread.sleep(1000);
                button.click();
                Thread.sleep(2000);
                
                flights = retryFindElements(".pIav2d", 20, null);
                Thread.sleep(500);
                js.executeScript("window.scrollBy(0,-10000)", "");
                scrollAmount = findLink(updatedFlight, scrollAmount, flights);
                returnedList.add(updatedFlight);
            }
            driver.quit();
            return returnedList;
        }
        catch(IndexOutOfBoundsException | IllegalArgumentException e) {
            e.printStackTrace();
            driver.quit();
            return returnedList;
        }
    }

        public List<UpdateFlightQuery> getJustPrice() throws InterruptedException {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            int scrollAmount = 0;
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            js.executeScript("window.scrollBy(0,1000)", "");
            WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".zISZ5c.QB2Jof")));
            button.click();
            Thread.sleep(2000);
            
            List<WebElement> flights = retryFindElements(".pIav2d", 20, null);

            js.executeScript("window.scrollBy(0,-10000)", "");
            Thread.sleep(1000);
            for(int i = 0; i < flights.size(); i++) {
                UpdateFlightQuery updateFlightQuery = new UpdateFlightQuery();
                updateFlightQuery = getPriceData(flights.get(i), updateFlightQuery, i);
                updateFlightQueryList.add(updateFlightQuery);
            }

            // mapping original flight list
            Map<String, Flight> originalFlightMap = flightList.stream()
                    .collect(Collectors.toMap(Flight::getFlightImpactLink, entry -> entry));

            List<Pair<UpdateFlightQuery, Flight>> differingPairs = updateFlightQueryList.stream()
            .filter(entryA -> {
                Flight entryB = originalFlightMap.get(entryA.getFlightImpactLink());
                return entryB != null && !(entryA.getPrice() == (entryB.getPrice()));
            })
            .map(entryA -> Pair.of(entryA, originalFlightMap.get(entryA.getFlightImpactLink())))
            .collect(Collectors.toList());

            for(Pair<UpdateFlightQuery, Flight> pair : differingPairs) {
                UpdateFlightQuery updatedFlight = pair.getLeft();
                Flight originalFlight = pair.getRight();
                updatedFlight.setId(originalFlight.getId());

                returnedList.add(updatedFlight);
            }
            driver.quit();
            return returnedList;
        }
        catch(IndexOutOfBoundsException | IllegalArgumentException e) {
            e.printStackTrace();
            driver.quit();
            return returnedList;
        }
    }

    public UpdateFlightQuery getPriceData(WebElement flightElement, UpdateFlightQuery updateFlightQuery, int iteration) throws InterruptedException {
        String input = null;
        int retries = 3;  // Number of times to retry finding the element if it becomes stale

        for (int attempt = 0; attempt < retries; attempt++) {
            try {
                // Try to get the text of the element
                input = flightElement.getText();
                break; // If successful, break out of the retry loop
            } catch (StaleElementReferenceException e) {
                // Re-locate the element since it might have become stale
                List<WebElement> flights = retryFindElements(".pIav2d", 20, null);
                flightElement = flights.get(iteration);
            }
        }
        
        double price = -1;
        String[] dataLines = input.split("\n");

        for(int i = 0; i < dataLines.length; i++) {
            // price
            if(dataLines[i].contains("$")) {
                String priceString = dataLines[i].substring(1);
                if(priceString.contains(",")) {
                    int indexOfComma = priceString.indexOf(",");
                    priceString = priceString.substring(0, indexOfComma) + priceString.substring(indexOfComma + 1);
                }
                price = Double.parseDouble(priceString);
                updateFlightQuery.setPrice(price);
                updateFlightQuery.setIteration(iteration);
            }
        }
    
        // travel impact link (for identifying the flights) 
        WebElement element = flightElement.findElement(By.cssSelector("div.NZRfve"));
        String url = element.getAttribute("data-travelimpactmodelwebsiteurl");
        updateFlightQuery.setFlightImpactLink(url);

        return updateFlightQuery;
    }

    public int findLink(UpdateFlightQuery updateFlightQuery, int scrollAmount, List<WebElement> flights) throws InterruptedException {
        int iteration = updateFlightQuery.getIteration();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int attempts = 0;
        String currentUrl = null;
        int addedScroll = 0;
        js.executeScript("window.scrollBy(0, " + scrollAmount + ")", "");
        boolean flightFound = false;
        WebElement flightElement = flights.get(iteration);

        while(attempts < 20) {
            try {
                Thread.sleep(1000);
                WebElement flightBox = flightElement.findElement(By.cssSelector(".gQ6yfe.m7VU8c"));
                flightFound = true;
                flightBox.click();
                break;
            }
            catch(NoSuchElementException | ElementNotInteractableException | TimeoutException e) {
                attempts++;
                addedScroll += 10;
                js.executeScript("window.scrollBy(0,20)", "");
                Thread.sleep(5);
            }
            catch(StaleElementReferenceException | StringIndexOutOfBoundsException e) {
                flights = retryFindElements(".pIav2d", 20, null);
                flightElement = flights.get(iteration);
            }
        }

        Thread.sleep(2000);
        String returnFlightPageHeader = "";
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); 

        try {
            boolean isTextPresent = wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".zBTtmb.ZSxxwc "), "eturn"));
            WebElement headerElement = driver.findElement(By.cssSelector(".zBTtmb.ZSxxwc "));
            returnFlightPageHeader = headerElement.getText();
        }
        catch(Exception e) { 
            e.printStackTrace();
        }

        Thread.sleep(2000);
        flights = retryFindElements(".pIav2d", 20, null);

        flights = retryFindElements(".pIav2d", 20, null);
        Thread.sleep(2000);
        
        WebElement returnFlightLink = retryFindElement(".gQ6yfe.m7VU8c", 20, flights.get(0));
        returnFlightLink.click();

        currentUrl = driver.getCurrentUrl();
        driver.get(link);
        updateFlightQuery.setLink(currentUrl);

        return addedScroll;
    }

    public static WebElement retryFindElement(String by, int attempts, WebElement mainElement) throws InterruptedException {
        try {
            for (int i = 0; i < attempts; i++) {
                try {
                    WebElement webElements;
    
                    if (mainElement == null) {
                        webElements = driver.findElement(By.cssSelector(by));
                    } 
                    else {
                        webElements = mainElement.findElement(By.cssSelector(by));
                    }

                    if(webElements == null) {
                        throw new NoSuchElementException();
                    }
    
                    return webElements;
                } 
                catch (NoSuchElementException | StaleElementReferenceException e) {
                    Thread.sleep(100);
                }
            }  
        }
        catch(Exception e) {
            System.out.println("can't find");
        }

        return null;
    }

    public static List<WebElement> retryFindElements(String by, int attempts, WebElement mainElement) throws InterruptedException {
        for (int i = 0; i < attempts; i++) {
            try {
                List<WebElement> webElements;

                if (mainElement == null) {
                    webElements = driver.findElements(By.cssSelector(by));
                } 
                else {
                    webElements = mainElement.findElements(By.cssSelector(by));
                }

                return webElements;
            } 
            catch (NoSuchElementException | StaleElementReferenceException e) {
                Thread.sleep(100);
            }
        }
        return null;
    }


}
