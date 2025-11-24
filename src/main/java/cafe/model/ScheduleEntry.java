package cafe.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "schedule_entries",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "entry_date"}),
                @UniqueConstraint(columnNames = {"schedule_month_id", "user_id", "entry_date"})
        })
public class ScheduleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_month_id", nullable = false)
    private ScheduleMonth scheduleMonth;


    public ScheduleEntry(User user, LocalDate entryDate, Status status) {
        this.user = user;
        this.entryDate = entryDate;
        this.status = status;
    }

    public enum Status {
        WORKING,
        OFF,
        VACATION,
        SICK_LEAVE
    }

}
