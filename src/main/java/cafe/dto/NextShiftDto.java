package cafe.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NextShiftDto {
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String cafeName;
    private Long daysUntil;
}
