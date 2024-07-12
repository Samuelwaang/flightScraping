package scraping.main;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/data")
public class ScrapeController {
    
    @GetMapping(path = "/get")
    public @ResponseBody List<Flight> getFlightData(@RequestBody ScrapeQuery request) throws InterruptedException {
        ScrapingService scraped = new ScrapingService(request.startPoint, request.destination, request.leaveDate, request.returnDate);
        return scraped.scrape();
    }
}
