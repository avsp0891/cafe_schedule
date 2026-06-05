package cafe.service;

import cafe.dto.FullScheduleDto;
import cafe.dto.MyScheduleDto;
import cafe.dto.NextShiftDto;
import cafe.exception.InsufficientPermissionsException;
import cafe.exception.ResourceNotFoundException;
import cafe.model.Cafe;
import cafe.model.ScheduleEntry;
import cafe.model.ScheduleMonth;
import cafe.model.User;
import cafe.repository.CafeRepository;
import cafe.repository.ScheduleEntryRepository;
import cafe.repository.ScheduleMonthRepository;
import cafe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScheduleService {
    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;
    @Autowired
    private ScheduleMonthRepository scheduleMonthRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CafeRepository cafeRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private ScheduleMonth getOrCreateScheduleMonth(YearMonth yearMonth, Cafe cafe) {
        return scheduleMonthRepository.findByYearAndMonthAndCafeId(yearMonth.getYear(), yearMonth.getMonthValue(), cafe.getId())
                .orElseGet(() -> scheduleMonthRepository.save(new ScheduleMonth(yearMonth, cafe)));
    }

    @Transactional
    public FullScheduleDto saveMySchedule(LocalDate monthDate, MyScheduleDto dto) {
        User user = getCurrentUser();
        Cafe cafe = cafeRepository.findById(dto.getCafeId())
                .orElseThrow(() -> new ResourceNotFoundException("Cafe not found"));

        if (!user.getCafes().contains(cafe)) {
            throw new InsufficientPermissionsException("You are not assigned to this cafe");
        }

        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth, cafe);

        if (month.isApproved()) {
            throw new IllegalStateException("Schedule is approved and locked");
        }

        validateNoDuplicateDates(dto.getShifts().stream().map(s -> s.getDate()).toList());

        scheduleEntryRepository.deleteByUserIdAndScheduleMonthIdAndCafeId(user.getId(), month.getId(), cafe.getId());
        scheduleEntryRepository.flush();

        for (MyScheduleDto.Shift shift : dto.getShifts()) {
            if (!YearMonth.from(shift.getDate()).equals(yearMonth)) {
                throw new IllegalArgumentException("Date " + shift.getDate() + " is not in month " + yearMonth);
            }
            if (!shift.getStartTime().isBefore(shift.getEndTime())) {
                throw new IllegalArgumentException("Start time must be before end time");
            }

            ScheduleEntry entry = new ScheduleEntry();
            entry.setUser(user);
            entry.setCafe(cafe);
            entry.setDate(shift.getDate());
            entry.setStartTime(shift.getStartTime());
            entry.setEndTime(shift.getEndTime());
            entry.setStatus(shift.getStatus());
            entry.setScheduleMonth(month);
            scheduleEntryRepository.save(entry);
        }

        return getMySchedule(monthDate, dto.getCafeId());
    }

    public FullScheduleDto getMySchedule(LocalDate monthDate, Long cafeId) {
        User user = getCurrentUser();
        Cafe cafe = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cafe not found"));

        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth, cafe);

        List<ScheduleEntry> myEntries = scheduleEntryRepository.findByUserIdAndScheduleMonthIdAndCafeId(user.getId(), month.getId(), cafe.getId());
        return new FullScheduleDto(cafeId, month.isApproved(), List.of(buildUserSchedule(user, myEntries)));
    }

    public FullScheduleDto getAllSchedule(LocalDate monthDate, Long cafeId) {
        YearMonth yearMonth = YearMonth.from(monthDate);
        Cafe cafe = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cafe not found"));
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth, cafe);

        List<ScheduleEntry> allEntries = scheduleEntryRepository.findByScheduleMonthIdAndCafeId(month.getId(), cafe.getId());
        Map<Long, List<ScheduleEntry>> grouped = allEntries.stream()
                .collect(Collectors.groupingBy(e -> e.getUser().getId()));

        List<FullScheduleDto.UserSchedule> userSchedules = grouped.values().stream()
                .map(entries -> buildUserSchedule(entries.get(0).getUser(), entries))
                .collect(Collectors.toList());

        return new FullScheduleDto(cafeId, month.isApproved(), userSchedules);
    }

    @Transactional
    public FullScheduleDto saveAllSchedule(LocalDate monthDate, FullScheduleDto dto) {
        YearMonth yearMonth = YearMonth.from(monthDate);
        Cafe cafe = cafeRepository.findById(dto.getCafeId())
                .orElseThrow(() -> new ResourceNotFoundException("Cafe not found"));
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth, cafe);

        if (month.isApproved()) {
            throw new IllegalStateException("Schedule is approved and locked");
        }

        for (FullScheduleDto.UserSchedule userSchedule : dto.getUserSchedules()) {
            validateNoDuplicateDates(userSchedule.getShifts().stream().map(s -> s.getDate()).toList());
        }

        scheduleEntryRepository.deleteAllByScheduleMonthIdAndCafeId(month.getId(), cafe.getId());
        scheduleEntryRepository.flush();

        for (FullScheduleDto.UserSchedule userSchedule : dto.getUserSchedules()) {
            User user = userRepository.findById(userSchedule.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            for (MyScheduleDto.Shift shift : userSchedule.getShifts()) {
                ScheduleEntry entry = new ScheduleEntry();
                entry.setUser(user);
                entry.setCafe(cafe);
                entry.setDate(shift.getDate());
                entry.setStartTime(shift.getStartTime());
                entry.setEndTime(shift.getEndTime());
                entry.setStatus(shift.getStatus());
                entry.setScheduleMonth(month);
                scheduleEntryRepository.save(entry);
            }
        }
        return getAllSchedule(monthDate, dto.getCafeId());
    }

    @Transactional
    public FullScheduleDto approveSchedule(LocalDate monthDate, Long cafeId, boolean approved) {
        YearMonth yearMonth = YearMonth.from(monthDate);
        Cafe cafe = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cafe not found"));
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth, cafe);
        month.setApproved(approved);
        month.setApprovedBy(getCurrentUser());
        scheduleMonthRepository.save(month);
        return getAllSchedule(monthDate, cafeId);
    }

    public boolean isApproved(LocalDate monthDate, Long cafeId) {
        YearMonth yearMonth = YearMonth.from(monthDate);
        Cafe cafe = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cafe not found"));
        return scheduleMonthRepository.findByYearAndMonthAndCafeId(yearMonth.getYear(), yearMonth.getMonthValue(), cafe.getId())
                .map(ScheduleMonth::isApproved)
                .orElse(false);
    }

    public NextShiftDto getMyNextShift() {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now();
        List<ScheduleEntry> entries = scheduleEntryRepository
                .findFirst1ByUserIdAndDateGreaterThanEqualAndStatusOrderByDateAscStartTimeAsc(
                        user.getId(), today, ScheduleEntry.Status.WORKING);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        ScheduleEntry e = entries.get(0);
        return NextShiftDto.builder()
                .date(e.getDate())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .cafeName(e.getCafe() != null ? e.getCafe().getName() : null)
                .daysUntil((long) (e.getDate().toEpochDay() - today.toEpochDay()))
                .build();
    }

    private void validateNoDuplicateDates(List<LocalDate> dates) {
        Set<LocalDate> unique = new java.util.HashSet<>();
        for (LocalDate d : dates) {
            if (!unique.add(d)) {
                throw new IllegalArgumentException("Duplicate shift date: " + d);
            }
        }
    }

    private FullScheduleDto.UserSchedule buildUserSchedule(User user, List<ScheduleEntry> entries) {
        FullScheduleDto.UserSchedule us = new FullScheduleDto.UserSchedule();
        us.setUserId(user.getId());
        us.setUsername(user.getUsername());
        us.setFirstName(user.getFirstName());
        us.setLastName(user.getLastName());
        us.setPosition(user.getPosition());
        us.setShifts(entries.stream().map(e -> {
            MyScheduleDto.Shift s = new MyScheduleDto.Shift();
            s.setDate(e.getDate());
            s.setStartTime(e.getStartTime());
            s.setEndTime(e.getEndTime());
            s.setStatus(e.getStatus());
            return s;
        }).collect(Collectors.toList()));
        return us;
    }
}