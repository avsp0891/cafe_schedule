package cafe.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.YearMonth;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "schedule_months",
        uniqueConstraints = @UniqueConstraint(columnNames = {"year", "month"}))
public class ScheduleMonth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private boolean approved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    public ScheduleMonth(YearMonth yearMonth) {
        this.year = yearMonth.getYear();
        this.month = yearMonth.getMonthValue();
    }

    public YearMonth getYearMonth() {
        return YearMonth.of(year, month);
    }
}
