package cafe.service;

import cafe.dto.FullScheduleDto;
import cafe.dto.MyScheduleDto;
import cafe.model.ScheduleEntry;
import cafe.model.ScheduleMonth;
import cafe.model.User;
import cafe.repository.ScheduleEntryRepository;
import cafe.repository.ScheduleMonthRepository;
import cafe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;
    @Autowired
    private ScheduleMonthRepository scheduleMonthRepository;
    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().contains(new SimpleGrantedAuthority(role));
    }

    private boolean isCafeAdmin() {
        return hasRole("CAFE_ADMIN");
    }

    private boolean isStaff() {
        return hasRole("STAFF");
    }

    private ScheduleMonth getOrCreateScheduleMonth(YearMonth yearMonth) {
        return scheduleMonthRepository.findByYearAndMonth(yearMonth.getYear(), yearMonth.getMonthValue())
                .orElseGet(() -> {
                    ScheduleMonth month = new ScheduleMonth(yearMonth);
                    return scheduleMonthRepository.save(month);
                });
    }

    // === СОТРУДНИК: получить своё расписание ===
    public FullScheduleDto getMySchedule(LocalDate monthDate) {
        if (!isStaff() && !isCafeAdmin()) {
            throw new RuntimeException("Access denied");
        }
        User currentUser = getCurrentUser();
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth);

        List<ScheduleEntry> myEntries = scheduleEntryRepository.findByUserIdAndScheduleMonthId(
                currentUser.getId(), month.getId()
        );

        // Формируем FullScheduleDto с одной записью
        FullScheduleDto.UserSchedule mySchedule = new FullScheduleDto.UserSchedule();
        mySchedule.setUserId(currentUser.getId());
        mySchedule.setUsername(currentUser.getUsername());
        mySchedule.setFirstName(currentUser.getFirstName());
        mySchedule.setLastName(currentUser.getLastName());
        mySchedule.setPosition(currentUser.getPosition());
        mySchedule.setDays(myEntries.stream()
                .map(entry -> {
                    MyScheduleDto.ScheduleDay day = new MyScheduleDto.ScheduleDay();
                    day.setDate(entry.getEntryDate());
                    day.setStatus(entry.getStatus());
                    return day;
                })
                .collect(Collectors.toList()));

        FullScheduleDto result = new FullScheduleDto();
        result.setUserSchedules(List.of(mySchedule));
        return result;
    }

    // === СОТРУДНИК: сохранить своё расписание ===
    @Transactional
    public void saveMySchedule(LocalDate monthDate, MyScheduleDto dto) {
        if (!isStaff()) {
            throw new RuntimeException("Only staff can save personal schedule");
        }
        User user = getCurrentUser();
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth);

        if (month.isApproved()) {
            throw new RuntimeException("Schedule is approved and locked");
        }

        // Проверка: все даты в одном месяце
        for (var day : dto.getDays()) {
            if (!YearMonth.from(day.getDate()).equals(yearMonth)) {
                throw new RuntimeException("All dates must be in the same month");
            }
        }

        // Удаляем старое
        scheduleEntryRepository.deleteByUserIdAndScheduleMonthId(user.getId(), month.getId());

        // Сохраняем новое
        for (var day : dto.getDays()) {
            ScheduleEntry entry = new ScheduleEntry();
            entry.setUser(user);
            entry.setEntryDate(day.getDate());
            entry.setStatus(day.getStatus());
            entry.setScheduleMonth(month);
            scheduleEntryRepository.save(entry);
        }
    }

    // === СОТРУДНИК И МЕНЕДЖЕР: получить всё расписание ===
    public FullScheduleDto getAllSchedule(LocalDate monthDate) {
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth);
        List<ScheduleEntry> allEntries = scheduleEntryRepository.findByScheduleMonthId(month.getId());

        // Группируем по пользователю
        Map<Long, List<ScheduleEntry>> grouped = allEntries.stream()
                .collect(Collectors.groupingBy(entry -> entry.getUser().getId()));

        List<FullScheduleDto.UserSchedule> userSchedules = grouped.entrySet().stream()
                .map(entry -> {
                    FullScheduleDto.UserSchedule us = new FullScheduleDto.UserSchedule();
                    User user = entry.getValue().get(0).getUser();
                    us.setUserId(user.getId());
                    us.setUsername(user.getUsername());
                    us.setFirstName(user.getFirstName());
                    us.setLastName(user.getLastName());
                    us.setPosition(user.getPosition());
                    us.setDays(entry.getValue().stream()
                            .map(e -> {
                                MyScheduleDto.ScheduleDay d = new MyScheduleDto.ScheduleDay();
                                d.setDate(e.getEntryDate());
                                d.setStatus(e.getStatus());
                                return d;
                            })
                            .collect(Collectors.toList()));
                    return us;
                })
                .collect(Collectors.toList());

        FullScheduleDto result = new FullScheduleDto();
        result.setUserSchedules(userSchedules);
        return result;
    }

    // === МЕНЕДЖЕР: сохранить всё расписание ===
    @Transactional
    public void saveAllSchedule(LocalDate monthDate, FullScheduleDto dto) {
        if (!isCafeAdmin()) {
            throw new RuntimeException("Only cafe admin can save full schedule");
        }
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth);

        // Удаляем всё расписание на месяц
        scheduleEntryRepository.deleteAllByScheduleMonthId(month.getId());

        // Сохраняем новое
        for (var userSchedule : dto.getUserSchedules()) {
            User user = userRepository.findById(userSchedule.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userSchedule.getUserId()));
            for (var day : userSchedule.getDays()) {
                if (!YearMonth.from(day.getDate()).equals(yearMonth)) {
                    throw new RuntimeException("Date out of month");
                }
                ScheduleEntry entry = new ScheduleEntry();
                entry.setUser(user);
                entry.setEntryDate(day.getDate());
                entry.setStatus(day.getStatus());
                entry.setScheduleMonth(month);
                scheduleEntryRepository.save(entry);
            }
        }
    }

    // === МЕНЕДЖЕР: утвердить месяц ===
    @Transactional
    public void approveSchedule(LocalDate monthDate) {
        if (!isCafeAdmin()) {
            throw new RuntimeException("Only cafe admin can approve schedule");
        }
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth);
        month.setApproved(true);
        month.setApprovedBy(getCurrentUser());
        scheduleMonthRepository.save(month);
    }

    // === Статус месяца (для фронтенда) ===
    public boolean isApproved(LocalDate monthDate) {
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = scheduleMonthRepository.findByYearAndMonth(
                yearMonth.getYear(), yearMonth.getMonthValue()
        ).orElse(null);
        return month != null && month.isApproved();
    }
}