package scraping.main;

import java.util.List;

import lombok.Data;

@Data
public class UpdateFlightQuery {
    private long id;
    private List<Flight> flights;
    private double price;
    // for identification
    private String flightImpactLink;
    private String link;
    private int iteration;
}
