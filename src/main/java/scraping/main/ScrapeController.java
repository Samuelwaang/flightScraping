package scraping.main;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/data")
public class ScrapeController {
    
    @PostMapping(path = "/get")
    public @ResponseBody ResponseEntity<List<Flight>> getFlightData(@RequestBody ScrapeQuery request) throws InterruptedException {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                ScrapingService scraped = new ScrapingService(request.startPoint, request.destination, request.leaveDate, request.returnDate);
                List<Flight> flights = scraped.scrape();
                return new ResponseEntity<>(flights, HttpStatus.OK);
            } 
            catch (Exception e) {
                attempt++;
                if (attempt >= maxRetries) {
                    // after 3 attempts send the exception
                    e.printStackTrace();
                    return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
                }
                try {
                    Thread.sleep(10000);
                } 
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); 
                    return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }
        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }   

    @PostMapping(path = "/update-price-link")
    public @ResponseBody ResponseEntity<List<UpdateFlightQuery>> updateFlightPriceAndLink(@RequestBody List<Flight> request) throws InterruptedException {
        try {
            ScrapingUpdateService scrapingUpdateService = new ScrapingUpdateService(request);
            List<UpdateFlightQuery> updateFlightQueries = scrapingUpdateService.checkNewPriceAndLink();
    
            return new ResponseEntity<>(updateFlightQueries, HttpStatus.OK);
        } 
        catch (Exception e) {
            e.printStackTrace();
    
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = "/update-price")
    public @ResponseBody ResponseEntity<List<UpdateFlightQuery>> updateFlightPrice(@RequestBody List<Flight> request) throws InterruptedException {
        try {
            ScrapingUpdateService scrapingUpdateService = new ScrapingUpdateService(request);
            List<UpdateFlightQuery> updateFlightQueries = scrapingUpdateService.getJustPrice();
    
            return new ResponseEntity<>(updateFlightQueries, HttpStatus.OK);
        } 
        catch (Exception e) {
            e.printStackTrace();
    
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}