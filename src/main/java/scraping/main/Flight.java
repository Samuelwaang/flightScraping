package scraping.main;

import lombok.Data;

@Data
public class Flight {
    private String airline;
    private int time;
    private double price;
    // FlightInfo, format of ("departs from;time;departs from;time...")
    private String location;
    private String link;
    private String flightStart;
    private String flightDestination;
    // the leaving date in this format ("month;day;hour;minute")
    private String leaveTime;
    private String arrivalTime;
    private String leaveDate;
    private String returnDay;
}
