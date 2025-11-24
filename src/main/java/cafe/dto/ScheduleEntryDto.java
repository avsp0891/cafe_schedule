package cafe.dto;

import cafe.model.ScheduleEntry;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class ScheduleEntryDto {

    private Long id;

    @NotNull
    private Long userId;

    @NotNull
    private LocalDate entryDate;

    @NotNull
    private ScheduleEntry.Status status;

}
