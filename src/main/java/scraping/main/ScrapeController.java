package scraping.main;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/data")
public class ScrapeController {
    
    @GetMapping(path = "/get")
    public @ResponseBody ResponseEntity<List<Flight>> getFlightData(@RequestBody ScrapeQuery request) throws InterruptedException {
        try{
            ScrapingService scraped = new ScrapingService(request.startPoint, request.destination, request.leaveDate, request.returnDate);
            List<Flight> flights = scraped.scrape();
            return new ResponseEntity<>(flights, HttpStatus.OK);
        }
        catch(Exception e) {
            e.printStackTrace();

            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
