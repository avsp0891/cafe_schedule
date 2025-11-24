package cafe.service;

import cafe.dto.*;
import cafe.model.*;
import cafe.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
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
    public List<ScheduleEntry> getMySchedule(LocalDate monthDate) {
        if (!isStaff() && !isCafeAdmin()) {
            throw new RuntimeException("Access denied");
        }
        User user = getCurrentUser();
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth);
        return scheduleEntryRepository.findByUserIdAndScheduleMonthId(user.getId(), month.getId());
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

    // === МЕНЕДЖЕР: получить всё расписание ===
    public FullScheduleDto getAllSchedule(LocalDate monthDate) {
        if (!isCafeAdmin()) {
            throw new RuntimeException("Only cafe admin can view full schedule");
        }
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