package cafe.dto;

import cafe.model.ScheduleEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class MyScheduleDto {
    @NotNull
    private Long cafeId;
    @NotEmpty
    @Valid
    private List<Shift> shifts;

    @Getter
    @Setter
    public static class Shift {
        @NotNull
        private LocalDate date;
        @NotNull
        private LocalTime startTime;
        @NotNull
        private LocalTime endTime;
        @NotNull
        private ScheduleEntry.Status status;
    }
}