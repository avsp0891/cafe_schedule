package cafe.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FullScheduleDto {
    @NotNull
    private Long cafeId;
    private boolean approved;
    private List<UserSchedule> userSchedules;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSchedule {
        private Long userId;
        private String username;
        private String firstName;
        private String lastName;
        private String position;
        private List<MyScheduleDto.Shift> shifts;
    }
}