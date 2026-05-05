package cafe.repository;

import cafe.model.ScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {
    List<ScheduleEntry> findByUserIdAndScheduleMonthIdAndCafeId(Long userId, Long scheduleMonthId, Long cafeId);

    List<ScheduleEntry> findByScheduleMonthIdAndCafeId(Long scheduleMonthId, Long cafeId);

    List<ScheduleEntry> findByUserIdAndCafeIdAndDate(Long userId, Long cafeId, java.time.LocalDate date);

    @Modifying
    @Query("DELETE FROM ScheduleEntry e WHERE e.user.id = :userId AND e.scheduleMonth.id = :monthId AND e.cafe.id = :cafeId")
    void deleteByUserIdAndScheduleMonthIdAndCafeId(@Param("userId") Long userId, @Param("monthId") Long monthId, @Param("cafeId") Long cafeId);

    @Modifying
    @Query("DELETE FROM ScheduleEntry e WHERE e.scheduleMonth.id = :monthId AND e.cafe.id = :cafeId")
    void deleteAllByScheduleMonthIdAndCafeId(@Param("monthId") Long monthId, @Param("cafeId") Long cafeId);
}