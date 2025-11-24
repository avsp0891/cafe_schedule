package cafe.dto;

import cafe.model.ScheduleEntry;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MyScheduleDto {
    private List<ScheduleDay> days;

    @Getter
    @Setter
    public static class ScheduleDay {
        private LocalDate date;
        private ScheduleEntry.Status status;

    }

}