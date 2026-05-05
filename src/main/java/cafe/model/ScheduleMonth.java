package cafe.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.YearMonth;

@Entity
@Table(name = "schedule_months",
        uniqueConstraints = @UniqueConstraint(columnNames = {"year", "month", "cafe_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafe_id", nullable = false)
    private Cafe cafe;

    public ScheduleMonth(YearMonth yearMonth, Cafe cafe) {
        this.year = yearMonth.getYear();
        this.month = yearMonth.getMonthValue();
        this.cafe = cafe;
    }

    public YearMonth getYearMonth() {
        return YearMonth.of(year, month);
    }
}