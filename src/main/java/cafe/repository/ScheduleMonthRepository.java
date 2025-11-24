package cafe.repository;

import cafe.model.ScheduleMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ScheduleMonthRepository extends JpaRepository<ScheduleMonth, Long> {
    Optional<ScheduleMonth> findByYearAndMonth(int year, int month);

}
