package cafe.dto;

import cafe.model.ScheduleEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserScheduleDto {
    private Long userId;
    private List<ScheduleDayDto> days;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDayDto {
        private LocalDate date;
        private ScheduleEntry.Status status;

    }
}
