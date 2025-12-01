package cafe.service;

import cafe.dto.FullScheduleDto;
import cafe.dto.MyScheduleDto;
import cafe.exception.InsufficientPermissionsException;
import cafe.exception.ResourceNotFoundException;
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
import java.util.ArrayList;
import java.util.HashMap;
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
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
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
            throw new InsufficientPermissionsException("Access denied: you must be staff or cafe admin");
        }
        User currentUser = getCurrentUser();
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth);

        List<ScheduleEntry> myEntries = scheduleEntryRepository.findByUserIdAndScheduleMonthId(
                currentUser.getId(), month.getId()
        );

        FullScheduleDto.UserSchedule mySchedule = buildUserSchedule(currentUser, myEntries);
        return new FullScheduleDto(List.of(mySchedule), month.isApproved());
    }

    // === СОТРУДНИК: сохранить своё расписание ===
    @Transactional
    public FullScheduleDto saveMySchedule(LocalDate monthDate, MyScheduleDto dto) {
        if (!isStaff() && !isCafeAdmin()) {
            throw new InsufficientPermissionsException("Only staff or cafe admin can save personal schedule");
        }
        User user = getCurrentUser();
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth);

        if (month.isApproved()) {
            throw new IllegalStateException("Schedule is approved and locked");
        }


        List<LocalDate> allDaysInMonth = getAllDaysInMonth(yearMonth);

        Map<LocalDate, ScheduleEntry.Status> inputMap = new HashMap<>();
        if (dto.getDays() != null) {
            for (MyScheduleDto.ScheduleDay day : dto.getDays()) {
                if (!YearMonth.from(day.getDate()).equals(yearMonth)) {
                    throw new IllegalArgumentException("Date " + day.getDate() + " is not in month " + yearMonth);
                }
                inputMap.put(day.getDate(), day.getStatus());
            }
        }

        scheduleEntryRepository.deleteByUserIdAndScheduleMonthId(user.getId(), month.getId());

        for (LocalDate date : allDaysInMonth) {
            ScheduleEntry.Status status = inputMap.getOrDefault(date, ScheduleEntry.Status.OFF);
            ScheduleEntry entry = new ScheduleEntry();
            entry.setUser(user);
            entry.setEntryDate(date);
            entry.setStatus(status);
            entry.setScheduleMonth(month);
            scheduleEntryRepository.save(entry);
        }

        List<ScheduleEntry> savedEntries = scheduleEntryRepository.findByUserIdAndScheduleMonthId(user.getId(), month.getId());
        FullScheduleDto.UserSchedule savedSchedule = buildUserSchedule(user, savedEntries);
        return new FullScheduleDto(List.of(savedSchedule), month.isApproved());
    }

    // === СОТРУДНИК И МЕНЕДЖЕР: получить всё расписание ===
    public FullScheduleDto getAllSchedule(LocalDate monthDate) {
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth);
        List<ScheduleEntry> allEntries = scheduleEntryRepository.findByScheduleMonthId(month.getId());

        // Группируем по пользователю
        Map<Long, List<ScheduleEntry>> grouped = allEntries.stream()
                .collect(Collectors.groupingBy(entry -> entry.getUser().getId()));

        List<FullScheduleDto.UserSchedule> userSchedules = grouped.values().stream()
                .map(entries -> {
                    User user = entries.get(0).getUser();
                    return buildUserSchedule(user, entries);
                })
                .collect(Collectors.toList());

        return new FullScheduleDto(userSchedules, month.isApproved());
    }

    // === МЕНЕДЖЕР: сохранить всё расписание ===
    @Transactional
    public FullScheduleDto saveAllSchedule(LocalDate monthDate, FullScheduleDto dto) {
        if (!isCafeAdmin()) {
            throw new InsufficientPermissionsException("Only cafe admin can save full schedule");
        }
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth);

        // Генерируем полный список дней месяца
        List<LocalDate> allDaysInMonth = getAllDaysInMonth(yearMonth);

        // Удаляем всё расписание на месяц
        scheduleEntryRepository.deleteAllByScheduleMonthId(month.getId());

        // Обрабатываем каждого пользователя из запроса
        if (dto.getUserSchedules() != null) {
            for (FullScheduleDto.UserSchedule userSchedule : dto.getUserSchedules()) {
                User user = userRepository.findById(userSchedule.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userSchedule.getUserId()));

                Map<LocalDate, ScheduleEntry.Status> inputMap = new HashMap<>();
                if (userSchedule.getDays() != null) {
                    for (MyScheduleDto.ScheduleDay day : userSchedule.getDays()) {
                        if (!YearMonth.from(day.getDate()).equals(yearMonth)) {
                            throw new IllegalArgumentException("Date " + day.getDate() + " is not in month " + yearMonth);
                        }
                        inputMap.put(day.getDate(), day.getStatus());
                    }
                }

                for (LocalDate date : allDaysInMonth) {
                    ScheduleEntry.Status status = inputMap.getOrDefault(date, ScheduleEntry.Status.OFF);
                    ScheduleEntry entry = new ScheduleEntry();
                    entry.setUser(user);
                    entry.setEntryDate(date);
                    entry.setStatus(status);
                    entry.setScheduleMonth(month);
                    scheduleEntryRepository.save(entry);
                }
            }
        }

        return getAllSchedule(monthDate);
    }

    // === МЕНЕДЖЕР: утвердить месяц ===
    @Transactional
    public FullScheduleDto approveSchedule(LocalDate monthDate, boolean approved) {
        if (!isCafeAdmin()) {
            throw new InsufficientPermissionsException("Only cafe admin can approve schedule");
        }
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = getOrCreateScheduleMonth(yearMonth);
        month.setApproved(approved);
        month.setApprovedBy(getCurrentUser());
        scheduleMonthRepository.save(month);
        return getAllSchedule(monthDate);
    }

    @Transactional
    public FullScheduleDto unapproveSchedule(LocalDate monthDate) {
        if (!isCafeAdmin()) {
            throw new InsufficientPermissionsException("Only cafe admin can unapprove schedule");
        }
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = scheduleMonthRepository.findByYearAndMonth(
                yearMonth.getYear(), yearMonth.getMonthValue()
        ).orElseThrow(() -> new ResourceNotFoundException("Schedule month not found"));

        month.setApproved(false);
        month.setApprovedBy(getCurrentUser()); // опционально: сбросить, кто утверждал
        scheduleMonthRepository.save(month);

        return getAllSchedule(monthDate);
    }

    // === Статус месяца (для фронтенда) ===
    public boolean isApproved(LocalDate monthDate) {
        YearMonth yearMonth = YearMonth.from(monthDate);
        ScheduleMonth month = scheduleMonthRepository.findByYearAndMonth(
                yearMonth.getYear(), yearMonth.getMonthValue()
        ).orElse(null);
        return month != null && month.isApproved();
    }

    private void validateDaysInMonth(List<MyScheduleDto.ScheduleDay> days, YearMonth expectedMonth) {
        for (var day : days) {
            if (!YearMonth.from(day.getDate()).equals(expectedMonth)) {
                throw new IllegalArgumentException("All dates must be in the same month: " + expectedMonth);
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
        us.setDays(entries.stream()
                .map(e -> {
                    MyScheduleDto.ScheduleDay d = new MyScheduleDto.ScheduleDay();
                    d.setDate(e.getEntryDate());
                    d.setStatus(e.getStatus());
                    return d;
                })
                .collect(Collectors.toList()));
        return us;
    }

    private List<LocalDate> getAllDaysInMonth(YearMonth yearMonth) {
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        List<LocalDate> days = new ArrayList<>();
        LocalDate current = firstDay;
        while (!current.isAfter(lastDay)) {
            days.add(current);
            current = current.plusDays(1);
        }
        return days;
    }
}