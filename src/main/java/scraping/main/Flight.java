package scraping.main;

import lombok.Data;
import jakarta.persistence.*;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
public class Flight {
    // @JsonIgnore
    private long id;
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
    private String tripLength;
    private boolean carryOnAllowed;

    // return flight data
    private String returnAirline;
    private String returnLeaveTime;
    private String returnArrivalTime;
    @CollectionTable(name = "return_flight_stops")
    private List<Stop> returnStops;
    private int returnNumStops;
    private int returnTime;
}
