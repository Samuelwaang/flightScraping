package scraping.main;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ScrapeQuery {
    String startPoint;
    String destination;
    String leaveDate;
    String returnDate;
}
