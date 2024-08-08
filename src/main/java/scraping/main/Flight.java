package scraping.main;

import lombok.Data;
import jakarta.persistence.*;
import java.util.List;

@Data
public class Flight {
    private String airline;
    private int time;
    private double price;
    private String link;
    private String flightStart;
    private String flightDestination;
    // the leaving date in this format ("month;day;hour;minute")
    private String leaveTime;
    private String arrivalTime;
    private String leaveDate;
    private String returnDay;
    @ElementCollection
    @CollectionTable(name = "flight_stops")
    private List<Stop> stops;
    private int numStops;
    private String flightImpactLink;
}
