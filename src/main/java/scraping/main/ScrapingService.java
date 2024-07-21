package scraping.main;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.Data;

@Data
public class ScrapingService {
    private static WebDriver driver;
    private String startPoint;
    private String destination;
    private String leaveDate;
    private String returnDate;
    private String link;
    private List<Flight> flightList = new ArrayList<Flight>();


    public ScrapingService(String startPoint, String destination, String leaveDate, String returnDate) {
        // uncomment for completely new startup only
        // WebDriverManager.chromedriver().setup(); 
        

        ChromeOptions options = new ChromeOptions();
        //options.setBinary("/usr/bin/google-chrome");
        options.addArguments("--headless");
        // options.addArguments("--no-sandbox");
        // options.addArguments("--disable-dev-shm-usage");
        // options.addArguments("--remote-allow-origins=*");


        options.addArguments("--disable-gpu"); // Applicable for Windows environment to avoid crash
        options.addArguments("--no-sandbox"); // Bypass OS security model
        options.addArguments("--disable-dev-shm-usage"); // Overcome limited resource problems
        options.addArguments("--window-size=1920x1080"); // Set window size to avoid element not interactable issues
        options.addArguments("--start-maximized");

        // WebDriverManager.chromedriver().setup();

        // ChromeOptions options = new ChromeOptions();
        // options.addArguments("--disable-gpu"); // Applicable for Windows environment to avoid crash
        // options.addArguments("--no-sandbox"); // Bypass OS security model
        // options.addArguments("--disable-dev-shm-usage"); // Overcome limited resource problems
        // options.addArguments("--window-size=1920x1080"); // Set window size to avoid element not interactable issues
        // options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);

        this.startPoint = startPoint;
        this.destination = destination;
        this.leaveDate = leaveDate;
        this.returnDate = returnDate;
        this.link = "https://www.google.com/travel/flights?q=flights+from+" + startPoint + "+to+" + destination + "+on+" + leaveDate + "+through+" + returnDate;

        driver.get(link);
    }


    public List<Flight> scrape() throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int scrollAmount = 0;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        js.executeScript("window.scrollBy(0,1000)", "");
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".zISZ5c.QB2Jof")));
        button.click();
        Thread.sleep(2000);
        
        List<WebElement> flights = retryFindElements(".pIav2d", 20, null);

        driver.get("https://www.google.com/travel/flights?q=flights+from+rno+to+san+on+2024-08-13+through+2024-08-15");

        for(int i = 0; i < flights.size(); i++) {
            Flight flight = new Flight();
            js.executeScript("window.scrollBy(0,10000)", "");
            button = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".zISZ5c.QB2Jof")));
            Thread.sleep(1000);
            button.click();
            Thread.sleep(2000);
            
            flights = retryFindElements(".pIav2d", 20, null);
            Thread.sleep(500);
            js.executeScript("window.scrollBy(0,-1000)", "");

            System.out.println(flights.get(i).getText());
            saveData(flight, flights.get(i));
            ScrollEntity scrollEntity = findLinkWithScroll(flights.get(i), scrollAmount, flight, i);
            scrollAmount = scrollEntity.getScroll();

            flight.setLink(scrollEntity.getLink());
            flight.setFlightStart(startPoint);
            flight.setFlightDestination(destination);
            flight.setLeaveDate(leaveDate);
            flight.setReturnDay(returnDate);
            System.out.println(flight);

            flightList.add(flight);
        }

        return flightList;
    }

    public ScrollEntity findLinkWithScroll(WebElement flightElement, int scrollAmount, Flight flight, int flightIteration) throws InterruptedException  {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int attempts = 0;
        String currentUrl = null;
        int addedScroll = 0;
        js.executeScript("window.scrollBy(0, " + scrollAmount + ")", "");
        boolean flightFound = false;

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
                List<WebElement> flights = retryFindElements(".pIav2d", 20, null);
                flightElement = flights.get(flightIteration);
            }
        }

        if(!flightFound) {
            System.out.println("Could not find the flight after " + attempts + " attempts.");
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
        List<WebElement> flights = retryFindElements(".pIav2d", 20, null);

        flights = retryFindElements(".pIav2d", 20, null);
        Thread.sleep(2000);
        
        WebElement returnFlightLink = retryFindElement(".gQ6yfe.m7VU8c", 20, flights.get(0));
        returnFlightLink.click();

        // finding airline if it says Separate tickets booked together
        if(flight.getAirline().contains("Separate")) {
            Thread.sleep(1000);
            flights = retryFindElements(".VfPpkd-WsjYwc.VfPpkd-WsjYwc-OWXEXe-INsAgc.KC1dQ.Usd1Ac.AaN0Dd.BCEBVd", 20, null);
            System.out.println(flights.size());
            Thread.sleep(2000);
            flight.setAirline(retryFindElement(".sSHqwe.tPgKwe.ogfYpf", 20, flights.get(0)).getText());
        }

        // finding airline for Self Transfer
        if(flight.getAirline().contains("Self")) {
            Thread.sleep(1000);
            flights = retryFindElements(".VfPpkd-WsjYwc.VfPpkd-WsjYwc-OWXEXe-INsAgc.KC1dQ.Usd1Ac.AaN0Dd.BCEBVd", 20, null);
            Thread.sleep(2000);
            String airlines = "";
            airlines += retryFindElement(".sSHqwe.tPgKwe.ogfYpf", 20, flights.get(0)).getText();
            for(int i = 1; i < flights.size(); i++) {
                airlines += " ," + retryFindElement(".sSHqwe.tPgKwe.ogfYpf", 20, flights.get(i)).getText();
            }
            flight.setAirline(airlines);
        }

        currentUrl = driver.getCurrentUrl();
        driver.get(link);

        return new ScrollEntity(currentUrl, addedScroll);
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

    public Flight saveData(Flight flight, WebElement flightData) throws InterruptedException {
        String input = flightData.getText();
        double price = -1;
        String[] dataLines = input.split("\n");

        // set airline
        flight.setAirline(dataLines[3]);
        for(int i = 0; i < dataLines.length; i++) {
            // price
            if(dataLines[i].contains("$")) {
                price = Double.parseDouble(dataLines[i].substring(1));
                flight.setPrice(price);
            }

            // duration of flight
            // if(dataLines[i].contains("hr")) {
            //     String[] timeSplit = dataLines[i].split(" ");
            //     int hours = 0;
            //     int min = 0;
            //     if(timeSplit.length == 2) {
            //         hours = Integer.parseInt(timeSplit[0]);
            //     }
            //     if(timeSplit.length == 4) {
            //         hours = Integer.parseInt(timeSplit[0]);
            //         min = Integer.parseInt(timeSplit[2]);
            //     }
            //     if(timeSplit.length == 2 | timeSplit.length == 4) {
            //         flight.setTime(hours * 60 + min);
            //     }
            // }

            // stops
            if(dataLines[i].contains("stop")) {
                String locations = startPoint + "-";
                if(!dataLines[i].equals("Nonstop")) {
                    int numStops = Integer.parseInt(dataLines[i].substring(0, 1));
                    for(int j = 0; j < numStops; j++) {
                        locations += dataLines[i + j] + "-";
                    }
                }
                locations += destination;
                flight.setLocation(locations);
            }
        }
        // leave time
        int indexToRemoveLeave = dataLines[0].length() - 3;
        String result1 = dataLines[0].substring(0, indexToRemoveLeave) + dataLines[0].substring(indexToRemoveLeave + 1);
        flight.setLeaveTime(result1);

        // arrival time
        int indexToRemoveArrive = -1;
        if(dataLines[2].contains("+1")) {
            indexToRemoveArrive = dataLines[2].length() - 5;
        }
        else{
            indexToRemoveArrive = dataLines[2].length() - 3;
        }
        String result2 = dataLines[2].substring(0, indexToRemoveArrive) + dataLines[2].substring(indexToRemoveArrive + 1);
        flight.setArrivalTime(result2);

        // duration
        String flightTime = driver.findElement(By.cssSelector("div.gvkrdb.AdWm1c.tPgKwe.ogfYpf")).getText();
        System.out.println("time element: " + flightTime);
        String[] timeSplit = flightTime.split(" ");
        int hours = 0;
        int min = 0;
        if(timeSplit.length == 2) {
            hours = Integer.parseInt(timeSplit[0]);
        }
        if(timeSplit.length == 4) {
            hours = Integer.parseInt(timeSplit[0]);
            min = Integer.parseInt(timeSplit[2]);
        }
        if(timeSplit.length == 2 | timeSplit.length == 4) {
            flight.setTime(hours * 60 + min);
        }
        
        return flight;
    }


    public static void main(String[] args) throws InterruptedException {
        ScrapingService test1 = new ScrapingService("rno", "san", "2024-08-13", "2024-08-13");
        test1.scrape();
    }
    
}
