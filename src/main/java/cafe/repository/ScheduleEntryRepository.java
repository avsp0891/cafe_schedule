package cafe.repository;

import cafe.model.ScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

import cafe.model.ScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {

    List<ScheduleEntry> findByUserIdAndScheduleMonthId(Long userId, Long scheduleMonthId);

    List<ScheduleEntry> findByScheduleMonthId(Long scheduleMonthId);

    @Modifying
    @Query("DELETE FROM ScheduleEntry e WHERE e.user.id = :userId AND e.scheduleMonth.id = :monthId")
    void deleteByUserIdAndScheduleMonthId(@Param("userId") Long userId, @Param("monthId") Long monthId);

    void deleteAllByScheduleMonthId(Long monthId);
}
