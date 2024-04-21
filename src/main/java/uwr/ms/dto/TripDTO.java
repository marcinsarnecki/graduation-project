package uwr.ms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class TripDTO {
    private Long id;
    private String name;
    private LocalDate startDate;
    private String location;
    private String description;
    private boolean isOwner;
}