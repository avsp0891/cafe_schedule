package cafe.utils;


import cafe.model.Role;
import cafe.model.ScheduleEntry;
import cafe.model.ScheduleMonth;
import cafe.model.User;
import cafe.repository.RoleRepository;
import cafe.repository.ScheduleEntryRepository;
import cafe.repository.ScheduleMonthRepository;
import cafe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class UserDataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    @Autowired
    private ScheduleMonthRepository scheduleMonthRepository;

    @Override
    public void run(String... args) {
        createRolesIfNotExist();
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

            userRepository.save(admin);
            System.out.println("✅ Admin user created: username=admin, password=admin123");
        }
    }

    private void createTestUsersAndScheduleIfNotExist() {
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
                userRepository.save(staff);
                System.out.println("✅ Staff user created: username=" + data[0] + ", password=staff123");
            }
        }

        // Создаём расписание для всех (manager + 2 staff)
        User manager = userRepository.findByUsername("manager").orElse(null);
        User barista1 = userRepository.findByUsername("barista1").orElse(null);
        User barista2 = userRepository.findByUsername("barista2").orElse(null);

        if (manager != null && barista1 != null && barista2 != null) {
            // Проверим, есть ли уже расписание на текущий месяц
            YearMonth currentMonth = YearMonth.now();
            YearMonth nextMonth = currentMonth.plusMonths(1);

            ScheduleMonth currentScheduleMonth = getOrCreateScheduleMonth(currentMonth);
            ScheduleMonth nextScheduleMonth = getOrCreateScheduleMonth(nextMonth);

            boolean currentExists = scheduleEntryRepository.findByScheduleMonthId(currentScheduleMonth.getId()).size() > 0;
            boolean nextExists = scheduleEntryRepository.findByScheduleMonthId(nextScheduleMonth.getId()).size() > 0;

            if (!currentExists) {
                createSampleScheduleForMonth(currentScheduleMonth, manager, barista1, barista2);
                System.out.println("✅ Sample schedule created for " + currentMonth);
            }
            if (!nextExists) {
                createSampleScheduleForMonth(nextScheduleMonth, manager, barista1, barista2);
                System.out.println("✅ Sample schedule created for " + nextMonth);
            }
        }
    }

    private ScheduleMonth getOrCreateScheduleMonth(YearMonth yearMonth) {
        return scheduleMonthRepository.findByYearAndMonth(yearMonth.getYear(), yearMonth.getMonthValue())
                .orElseGet(() -> {
                    ScheduleMonth month = new ScheduleMonth(yearMonth);
                    return scheduleMonthRepository.save(month);
                });
    }

    private void createSampleScheduleForMonth(ScheduleMonth scheduleMonth, User... users) {
        YearMonth yearMonth = scheduleMonth.getYearMonth();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Генерируем расписание: чередуем WORKING и OFF
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (User user : users) {
                ScheduleEntry.Status status;
                if (user.getUsername().equals("manager")) {
                    // Менеджер работает чаще
                    status = date.getDayOfMonth() % 3 == 0 ? ScheduleEntry.Status.OFF : ScheduleEntry.Status.WORKING;
                } else {
                    // Сотрудники: работают 2 дня, отдыхают 2
                    int dayMod = date.getDayOfMonth() % 4;
                    if (dayMod == 1 || dayMod == 2) {
                        status = ScheduleEntry.Status.WORKING;
                    } else if (dayMod == 3) {
                        status = ScheduleEntry.Status.VACATION;
                    } else {
                        status = ScheduleEntry.Status.OFF;
                    }
                }

                ScheduleEntry entry = new ScheduleEntry();
                entry.setUser(user);
                entry.setEntryDate(date);
                entry.setStatus(status);
                entry.setScheduleMonth(scheduleMonth);
                scheduleEntryRepository.save(entry);
            }
        }
    }
}