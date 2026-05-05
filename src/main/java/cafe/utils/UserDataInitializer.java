package cafe.utils;

import cafe.model.*;
import cafe.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class UserDataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CafeRepository cafeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    @Autowired
    private ScheduleMonthRepository scheduleMonthRepository;

    @Override
    @Transactional
    public void run(String... args) {
        createRolesIfNotExist();
        createCafesIfNotExist();
        createAdminUserIfNotExist();
        createTestUsersAndScheduleIfNotExist();
    }

    private void createRolesIfNotExist() {
        if (roleRepository.count() == 0) {
            Arrays.stream(Role.ERole.values())
                    .map(Role::new)
                    .forEach(roleRepository::save);
            System.out.println("✅ Roles created: USER_ADMIN, CAFE_ADMIN, STAFF");
        }
    }

    private void createCafesIfNotExist() {
        if (cafeRepository.count() == 0) {
            Cafe cafe1 = new Cafe();
            cafe1.setName("Кофе Хаус Центр");
            cafe1.setAddress("ул. Ленина, 10");
            cafeRepository.save(cafe1);

            Cafe cafe2 = new Cafe();
            cafe2.setName("Кофе Хаус Парк");
            cafe2.setAddress("пр. Мира, 45");
            cafeRepository.save(cafe2);

            System.out.println("✅ Cafes created: Кофе Хаус Центр, Кофе Хаус Парк");
        }
    }

    private void createAdminUserIfNotExist() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Админ");
            admin.setLastName("Системы");
            admin.setPosition("Администратор пользователей");

            Set<Role> roles = new HashSet<>();
            roles.add(roleRepository.findByName(Role.ERole.USER_ADMIN)
                    .orElseThrow(() -> new RuntimeException("USER_ADMIN role not found")));
            admin.setRoles(roles);

            // Админ имеет доступ ко всем кафе
            admin.setCafes(new HashSet<>(cafeRepository.findAll()));

            userRepository.save(admin);
            System.out.println("✅ Admin user created: username=admin, password=admin123");
        }
    }

    @Transactional
    protected void createTestUsersAndScheduleIfNotExist() {
        // Получаем кафе для привязки
        List<Cafe> cafes = cafeRepository.findAll();
        if (cafes.isEmpty()) return;
        Cafe mainCafe = cafes.get(0);

        // Создаём менеджера
        if (!userRepository.existsByUsername("manager")) {
            User manager = new User();
            manager.setUsername("manager");
            manager.setEmail("manager@cafe.com");
            manager.setPassword(passwordEncoder.encode("manager123"));
            manager.setFirstName("Иван");
            manager.setLastName("Петров");
            manager.setPosition("Менеджер кафе");

            Set<Role> managerRoles = new HashSet<>();
            managerRoles.add(roleRepository.findByName(Role.ERole.CAFE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("CAFE_ADMIN role not found")));
            manager.setRoles(managerRoles);
            manager.setCafes(Set.of(mainCafe)); // Менеджер привязан к основному кафе

            userRepository.save(manager);
            System.out.println("✅ Cafe manager created: username=manager, password=manager123");
        }

        // Создаём сотрудников
        String[][] staffData = {
                {"barista1", "barista1@cafe.com", "Анна", "Смирнова", "Бариста"},
                {"barista2", "barista2@cafe.com", "Дмитрий", "Козлов", "Помощник повара"}
        };

        for (String[] data : staffData) {
            if (!userRepository.existsByUsername(data[0])) {
                User staff = new User();
                staff.setUsername(data[0]);
                staff.setEmail(data[1]);
                staff.setPassword(passwordEncoder.encode("staff123"));
                staff.setFirstName(data[2]);
                staff.setLastName(data[3]);
                staff.setPosition(data[4]);

                Set<Role> staffRoles = new HashSet<>();
                staffRoles.add(roleRepository.findByName(Role.ERole.STAFF)
                        .orElseThrow(() -> new RuntimeException("STAFF role not found")));
                staff.setRoles(staffRoles);
                staff.setCafes(Set.of(mainCafe)); // Сотрудники привязаны к основному кафе

                userRepository.save(staff);
                System.out.println("✅ Staff user created: username=" + data[0] + ", password=staff123");
            }
        }

        // Создаём расписание для всех пользователей
        User manager = userRepository.findByUsername("manager").orElse(null);
        User barista1 = userRepository.findByUsername("barista1").orElse(null);
        User barista2 = userRepository.findByUsername("barista2").orElse(null);

        if (manager != null && barista1 != null && barista2 != null) {
            YearMonth currentMonth = YearMonth.now();
            YearMonth nextMonth = currentMonth.plusMonths(1);

            // Создаём расписание для основного кафе
            ScheduleMonth currentScheduleMonth = getOrCreateScheduleMonth(currentMonth, mainCafe);
            ScheduleMonth nextScheduleMonth = getOrCreateScheduleMonth(nextMonth, mainCafe);

            boolean currentExists = !scheduleEntryRepository
                    .findByScheduleMonthIdAndCafeId(currentScheduleMonth.getId(), mainCafe.getId()).isEmpty();
            boolean nextExists = !scheduleEntryRepository
                    .findByScheduleMonthIdAndCafeId(nextScheduleMonth.getId(), mainCafe.getId()).isEmpty();

            if (!currentExists) {
                createSampleScheduleForMonth(currentScheduleMonth, mainCafe, List.of(manager, barista1, barista2));
                System.out.println("✅ Sample schedule created for " + currentMonth + " at " + mainCafe.getName());
            }
            if (!nextExists) {
                createSampleScheduleForMonth(nextScheduleMonth, mainCafe, List.of(manager, barista1, barista2));
                System.out.println("✅ Sample schedule created for " + nextMonth + " at " + mainCafe.getName());
            }
        }
    }

    private ScheduleMonth getOrCreateScheduleMonth(YearMonth yearMonth, Cafe cafe) {
        return scheduleMonthRepository.findByYearAndMonthAndCafeId(
                        yearMonth.getYear(), yearMonth.getMonthValue(), cafe.getId())
                .orElseGet(() -> {
                    ScheduleMonth month = new ScheduleMonth(yearMonth, cafe);
                    return scheduleMonthRepository.save(month);
                });
    }

    private void createSampleScheduleForMonth(ScheduleMonth scheduleMonth, Cafe cafe, List<User> users) {
        YearMonth yearMonth = scheduleMonth.getYearMonth();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Временные слоты для смен
        LocalTime[] shifts = {
                LocalTime.of(8, 0),   // Утренняя
                LocalTime.of(12, 0),  // Дневная
                LocalTime.of(16, 0)   // Вечерняя
        };

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);

                // Логика распределения смен
                ScheduleEntry.Status status;
                LocalTime startTime = null;
                LocalTime endTime = null;

                if (user.getUsername().equals("manager")) {
                    // Менеджер: работает 5 дней в неделю, дневная смена
                    if (date.getDayOfWeek().getValue() <= 5) {
                        status = ScheduleEntry.Status.WORKING;
                        startTime = LocalTime.of(10, 0);
                        endTime = LocalTime.of(19, 0);
                    } else {
                        status = ScheduleEntry.Status.OFF;
                    }
                } else {
                    // Сотрудники: чередование смен
                    int dayMod = (date.getDayOfMonth() + i) % 5;
                    if (dayMod < 3) {
                        status = ScheduleEntry.Status.WORKING;
                        // Разные смены для разных сотрудников
                        startTime = shifts[(i + date.getDayOfMonth()) % shifts.length];
                        endTime = startTime.plusHours(8);
                    } else if (dayMod == 3) {
                        status = ScheduleEntry.Status.VACATION;
                        startTime = LocalTime.of(9, 0);
                        endTime = LocalTime.of(13, 0); // Пол-дня
                    } else {
                        status = ScheduleEntry.Status.OFF;
                    }
                }

                // Создаём запись только если статус не OFF (или создаём с OFF для явного указания выходного)
                if (status != ScheduleEntry.Status.OFF) {
                    ScheduleEntry entry = new ScheduleEntry();
                    entry.setUser(user);
                    entry.setCafe(cafe);
                    entry.setDate(date);
                    entry.setStartTime(startTime);
                    entry.setEndTime(endTime);
                    entry.setStatus(status);
                    entry.setScheduleMonth(scheduleMonth);
                    scheduleEntryRepository.save(entry);
                }
            }
        }
    }
}