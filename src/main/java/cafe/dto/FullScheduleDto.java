package cafe.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FullScheduleDto {
    private List<UserSchedule> userSchedules;

    @Getter
    @Setter
    public static class UserSchedule {
        private Long userId;
        private String username;
        private String firstName;
        private String lastName;
        private String position;
        private List<MyScheduleDto.ScheduleDay> days;

    }

}
