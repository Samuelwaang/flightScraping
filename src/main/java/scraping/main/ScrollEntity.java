package scraping.main;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScrollEntity {
    private String link;
    private int scroll;
}
