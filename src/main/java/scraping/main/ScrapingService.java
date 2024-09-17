package scraping.main;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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

        options.addArguments("--headless");
        options.addArguments("--disable-gpu"); // Applicable for Windows environment to avoid crash
        options.addArguments("--no-sandbox"); // Bypass OS security model
        options.addArguments("--disable-dev-shm-usage"); // Overcome limited resource problems
        options.addArguments("--window-size=1920x1080"); // Set window size to avoid element not interactable issues
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);

        this.startPoint = startPoint;
        this.destination = destination;
        this.leaveDate = leaveDate;
        this.returnDate = returnDate;
        this.link = "https://www.google.com/travel/flights?q=flights+from+" + startPoint + "+to+" + destination + "+on+"
                + leaveDate + "+through+" + returnDate;

        System.out.println(link);
        driver.get(link);
    }

    public List<Flight> scrape() throws InterruptedException {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            int scrollAmount = 0;
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            js.executeScript("window.scrollBy(0,1000)", "");
            WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".zISZ5c.QB2Jof")));
            button.click();
            Thread.sleep(2000);
            List<WebElement> flights = wait
                    .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector(".pIav2d")));

            driver.get(link);

            // 10 iterations for testing, normally use flights.size()
            System.out.println(flights.size());
            for (int i = 0; i < flights.size(); i++) {
                try {
                    Thread.sleep(250);
                    Flight flight = new Flight();
                    js.executeScript("window.scrollBy(0,10000)", "");
                    button = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".zISZ5c.QB2Jof")));
                    Thread.sleep(1000);
                    button.click();
                    Thread.sleep(2000);

                    flights = retryFindElements(".pIav2d", 20, null);
                    Thread.sleep(500);
                    js.executeScript("window.scrollBy(0,-10000)", "");

                    saveData(flight, flights.get(i));
                    ScrollEntity scrollEntity = findLinkWithScroll(flights.get(i), scrollAmount, flight, i);
                    scrollAmount = scrollEntity.getScroll();

                    flight.setLink(scrollEntity.getLink());
                    flight.setFlightStart(startPoint);
                    flight.setFlightDestination(destination);
                    flight.setLeaveDate(leaveDate);
                    flight.setReturnDay(returnDate);

                    flightList.add(flight);
                } catch (NoSuchElementException e) {
                    continue;
                }
            }
            driver.quit();
            return flightList;
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            e.printStackTrace();
            driver.quit();
            return flightList;
        }
    }

    public ScrollEntity findLinkWithScroll(WebElement flightElement, int scrollAmount, Flight flight,
            int flightIteration) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int attempts = 0;
        String currentUrl = null;
        int addedScroll = 0;
        js.executeScript("window.scrollBy(0, " + scrollAmount + ")", "");
        boolean flightFound = false;

        while (attempts < 20) {
            try {
                Thread.sleep(1000);
                WebElement flightBox = flightElement.findElement(By.cssSelector(".gQ6yfe.m7VU8c"));
                flightFound = true;
                flightBox.click();
                break;
            } catch (NoSuchElementException | ElementNotInteractableException | TimeoutException e) {
                attempts++;
                addedScroll += 10;
                js.executeScript("window.scrollBy(0,20)", "");
                Thread.sleep(5);
            } catch (StaleElementReferenceException | StringIndexOutOfBoundsException e) {
                List<WebElement> flights = retryFindElements(".pIav2d", 20, null);
                flightElement = flights.get(flightIteration);
            }
        }

        if (!flightFound) {
            System.out.println("Could not find the flight after " + attempts + " attempts.");
        }

        Thread.sleep(2000);
        String returnFlightPageHeader = "";
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            boolean isTextPresent = wait.until(
                    ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".zBTtmb.ZSxxwc "), "eturn"));
            WebElement headerElement = driver.findElement(By.cssSelector(".zBTtmb.ZSxxwc "));
            returnFlightPageHeader = headerElement.getText();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread.sleep(2000);
        List<WebElement> flights = retryFindElements(".pIav2d", 20, null);

        flights = retryFindElements(".pIav2d", 20, null);
        Thread.sleep(2000);

        WebElement returnFlightLink = retryFindElement(".gQ6yfe.m7VU8c", 20, flights.get(0));

        // setting return flight data
        Thread.sleep(250);
        String input = returnFlightLink.getText();
        String[] dataLines = input.split("\n");

        flight.setReturnAirline(dataLines[3]);

        for (int i = 0; i < dataLines.length; i++) {
            // stops
            if (dataLines[i].contains("stop")) {
                List<Stop> stops = new ArrayList<>();
                int numStops = 0;
                if (!dataLines[i].equals("Nonstop")) {
                    numStops = Integer.parseInt(dataLines[i].substring(0, 1));
                    if (numStops == 1) {
                        String stopsString = dataLines[i + 1];
                        stops.add(getStopDuration(stopsString));
                    } else {
                        WebElement flightInfoButton = retryFindElement("button.VfPpkd-LgbsSe.nCP5yc.AjY5Oe", 20,
                                returnFlightLink);
                        flightInfoButton.click();

                        List<WebElement> multipleFlightStops = retryFindElements("div.tvtJdb.eoY5cb.y52p7d", 20,
                                returnFlightLink);

                        for (WebElement stopElement : multipleFlightStops) {
                            String layoverStringSplit[] = stopElement.getText().split("layover");
                            String location = layoverStringSplit[1].trim();
                            location = location.substring(location.indexOf('(') + 1, location.indexOf(')'));

                            Stop stop = new Stop();
                            stop.setTime(getMultipleStopDuration(layoverStringSplit[0].trim()));
                            stop.setLocation(location);
                            stops.add(stop);
                        }

                        flightInfoButton.click();
                    }
                }
                flight.setReturnStops(stops);
                flight.setReturnNumStops(numStops);
            }
        }

        // leave time
        int indexToRemoveLeave = dataLines[0].length() - 3;
        String result1 = dataLines[0].substring(0, indexToRemoveLeave) + dataLines[0].substring(indexToRemoveLeave + 1);
        flight.setReturnLeaveTime(result1);

        // arrival time
        int indexToRemoveArrive = -1;
        if (dataLines[2].contains("+1")) {
            indexToRemoveArrive = dataLines[2].length() - 5;
        } else {
            indexToRemoveArrive = dataLines[2].length() - 3;
        }
        String result2 = dataLines[2].substring(0, indexToRemoveArrive)
                + dataLines[2].substring(indexToRemoveArrive + 1);
        flight.setReturnArrivalTime(result2);

        // duration
        String flightTime = returnFlightLink.findElement(By.cssSelector("div.gvkrdb.AdWm1c.tPgKwe.ogfYpf")).getText();
        String[] timeSplit = flightTime.split(" ");
        int hours = 0;
        int min = 0;
        if (timeSplit.length == 2) {
            hours = Integer.parseInt(timeSplit[0]);
        }
        if (timeSplit.length == 4) {
            hours = Integer.parseInt(timeSplit[0]);
            min = Integer.parseInt(timeSplit[2]);
        }
        if (timeSplit.length == 2 | timeSplit.length == 4) {
            flight.setReturnTime(hours * 60 + min);
        }

        returnFlightLink.click();

        // finding airline if it says Separate tickets booked together
        if (flight.getAirline().contains("Separate")) {
            Thread.sleep(1000);
            flights = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("VfPpkd-WsjYwc")));
            Thread.sleep(2000);
            flights = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("VfPpkd-WsjYwc")));
            flight.setAirline(retryFindElement(".sSHqwe.tPgKwe.ogfYpf", 20, flights.get(0)).getText());
        }

        // finding airline for Self Transfer
        if (flight.getAirline().contains("Self")) {
            Thread.sleep(1000);
            flights = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("VfPpkd-WsjYwc")));
            Thread.sleep(2000);
            flights = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("VfPpkd-WsjYwc")));
            String airlines = "";
            airlines += retryFindElement(".sSHqwe.tPgKwe.ogfYpf", 20, flights.get(0)).getText();
            for (int i = 1; i < flights.size(); i++) {
                airlines += " ," + retryFindElement(".sSHqwe.tPgKwe.ogfYpf", 20, flights.get(i)).getText();
            }
            flight.setAirline(airlines);
        }

        // also finding airlines for same issues as above but for return flight
        // Seperate
        if (flight.getReturnAirline().contains("Separate")) {
            Thread.sleep(1000);
            flights = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("VfPpkd-WsjYwc")));
            Thread.sleep(2000);
            flights = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("VfPpkd-WsjYwc")));
            flight.setReturnAirline(retryFindElement(".sSHqwe.tPgKwe.ogfYpf", 20, flights.get(1)).getText());
        }

        // Self Transfer
        if (flight.getReturnAirline().contains("Self")) {
            Thread.sleep(1000);
            flights = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("VfPpkd-WsjYwc")));
            Thread.sleep(2000);
            flights = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("VfPpkd-WsjYwc")));
            String airlines = "";
            airlines += retryFindElement(".sSHqwe.tPgKwe.ogfYpf", 20, flights.get(1)).getText();
            for (int i = 1; i < flights.size(); i++) {
                airlines += " ," + retryFindElement(".sSHqwe.tPgKwe.ogfYpf", 20, flights.get(i)).getText();
            }
            flight.setReturnAirline(airlines);
        }

        currentUrl = driver.getCurrentUrl();
        driver.get(link);

        return new ScrollEntity(currentUrl, addedScroll);
    }

    public static WebElement retryFindElement(String by, int attempts, WebElement mainElement)
            throws InterruptedException {
        try {
            for (int i = 0; i < attempts; i++) {
                try {
                    WebElement webElements;

                    if (mainElement == null) {
                        webElements = driver.findElement(By.cssSelector(by));
                    } else {
                        webElements = mainElement.findElement(By.cssSelector(by));
                    }

                    if (webElements == null) {
                        throw new NoSuchElementException();
                    }

                    return webElements;
                } catch (NoSuchElementException | StaleElementReferenceException e) {
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            System.out.println("can't find");
        }

        return null;
    }

    public static List<WebElement> retryFindElements(String by, int attempts, WebElement mainElement)
            throws InterruptedException {
        for (int i = 0; i < attempts; i++) {
            try {
                List<WebElement> webElements;

                if (mainElement == null) {
                    webElements = driver.findElements(By.cssSelector(by));
                } else {
                    webElements = mainElement.findElements(By.cssSelector(by));
                }

                return webElements;
            } catch (NoSuchElementException | StaleElementReferenceException e) {
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
        for (int i = 0; i < dataLines.length; i++) {
            // price
            if (dataLines[i].contains("$")) {
                String priceString = dataLines[i].substring(1);
                if (priceString.contains(",")) {
                    int indexOfComma = priceString.indexOf(",");
                    priceString = priceString.substring(0, indexOfComma) + priceString.substring(indexOfComma + 1);
                }
                price = Double.parseDouble(priceString);
                flight.setPrice(price);
            }

            // stops
            if (dataLines[i].contains("stop")) {
                List<Stop> stops = new ArrayList<>();
                int numStops = 0;
                if (!dataLines[i].equals("Nonstop")) {
                    numStops = Integer.parseInt(dataLines[i].substring(0, 1));
                    if (numStops == 1) {
                        String stopsString = dataLines[i + 1];
                        stops.add(getStopDuration(stopsString));
                    } else {
                        WebElement flightInfoButton = retryFindElement("button.VfPpkd-LgbsSe.nCP5yc.AjY5Oe", 20,
                                flightData);
                        flightInfoButton.click();

                        List<WebElement> multipleFlightStops = retryFindElements("div.tvtJdb.eoY5cb.y52p7d", 20,
                                flightData);

                        for (WebElement stopElement : multipleFlightStops) {
                            Thread.sleep(100);

                            String layoverStringSplit[] = stopElement.getText().split("layover");
                            String location = layoverStringSplit[1].trim();
                            location = location.substring(location.indexOf('(') + 1, location.indexOf(')'));

                            Stop stop = new Stop();
                            stop.setTime(getMultipleStopDuration(layoverStringSplit[0].trim()));
                            stop.setLocation(location);
                            stops.add(stop);
                        }

                        flightInfoButton.click();
                    }
                }
                flight.setStops(stops);
                flight.setNumStops(numStops);
                int stopTotalDuration = 0;
                // total stop duration
                for (Stop stop : stops) {
                    stopTotalDuration += stop.getTime();
                }
                flight.setTotalStopDuration(stopTotalDuration);
            }
        }
        // leave time
        int indexToRemoveLeave = dataLines[0].length() - 3;
        String result1 = dataLines[0].substring(0, indexToRemoveLeave) + dataLines[0].substring(indexToRemoveLeave + 1);
        flight.setLeaveTime(result1);

        // arrival time
        int indexToRemoveArrive = -1;
        if (dataLines[2].contains("+1")) {
            indexToRemoveArrive = dataLines[2].length() - 5;
        } else {
            indexToRemoveArrive = dataLines[2].length() - 3;
        }
        String result2 = dataLines[2].substring(0, indexToRemoveArrive)
                + dataLines[2].substring(indexToRemoveArrive + 1);
        flight.setArrivalTime(result2);

        // duration
        String flightTime = flightData.findElement(By.cssSelector("div.gvkrdb.AdWm1c.tPgKwe.ogfYpf")).getText();
        String[] timeSplit = flightTime.split(" ");
        int hours = 0;
        int min = 0;
        if (timeSplit.length == 2) {
            hours = Integer.parseInt(timeSplit[0]);
        }
        if (timeSplit.length == 4) {
            hours = Integer.parseInt(timeSplit[0]);
            min = Integer.parseInt(timeSplit[2]);
        }
        if (timeSplit.length == 2 | timeSplit.length == 4) {
            flight.setTime(hours * 60 + min);
        }

        // travel impact link (for identifying the flights)
        WebElement element = flightData.findElement(By.cssSelector("div.NZRfve"));
        String url = element.getAttribute("data-travelimpactmodelwebsiteurl");
        flight.setFlightImpactLink(url);

        // check if there is a free carry on
        WebElement carryOn = flightData.findElement(By.cssSelector("div.JMnxgf"));
        if (!carryOn.getText().trim().isEmpty() || !carryOn.findElements(By.xpath("./*")).isEmpty()) {
            flight.setCarryOnAllowed(true);
        } else {
            flight.setCarryOnAllowed(false);
        }

        return flight;
    }

    private static Stop getStopDuration(String stopsString) {
        String[] stopsStringSplit = stopsString.split(" ");
        int hours = 0;
        int min = 0;
        Stop stop = new Stop();

        if (stopsStringSplit.length == 3) {
            stop = new Stop();
            // sets the stop's location
            stop.setLocation(stopsStringSplit[2]);
            // sets the duration of the stop
            if (stopsStringSplit[1].contains("hr")) {
                hours = Integer.parseInt(stopsStringSplit[0]);
            }
            if (stopsStringSplit[1].contains("min")) {
                min = Integer.parseInt(stopsStringSplit[0]);
            }
        }
        if (stopsStringSplit.length == 5) {
            stop = new Stop();
            // sets the stop's location
            stop.setLocation(stopsStringSplit[4]);
            // sets the duration of the stop
            hours = Integer.parseInt(stopsStringSplit[0]);
            min = Integer.parseInt(stopsStringSplit[2]);
        }
        stop.setTime(hours * 60 + min);

        return stop;
    }

    private static int getMultipleStopDuration(String stopsString) {
        String[] stopsStringSplit = stopsString.split(" ");
        int hours = 0;
        int min = 0;

        if (stopsStringSplit.length == 2) {
            // sets the duration of the stop
            if (stopsStringSplit[1].contains("hr")) {
                hours = Integer.parseInt(stopsStringSplit[0]);
            }
            if (stopsStringSplit[1].contains("min")) {
                min = Integer.parseInt(stopsStringSplit[0]);
            }

        }
        if (stopsStringSplit.length == 4) {
            hours = Integer.parseInt(stopsStringSplit[0]);
            min = Integer.parseInt(stopsStringSplit[2]);
        }

        return hours * 60 + min;
    }

}
