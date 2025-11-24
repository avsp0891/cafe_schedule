package cafe.utils;


import cafe.model.Role;
import cafe.model.User;
import cafe.repository.RoleRepository;
import cafe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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

    @Override
    public void run(String... args) {
        // Создаём роли, если их нет
        if (!roleRepository.existsById(1L)) {
            Role userAdmin = new Role(Role.ERole.USER_ADMIN);
            Role cafeAdmin = new Role(Role.ERole.CAFE_ADMIN);
            Role staff = new Role(Role.ERole.STAFF);
            roleRepository.save(userAdmin);
            roleRepository.save(cafeAdmin);
            roleRepository.save(staff);
        }

        // Создаём админа, если его нет
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin123")); // ← настоящий пароль!

            Set<Role> roles = new HashSet<>();
            roles.add(roleRepository.findByName(Role.ERole.USER_ADMIN)
                    .orElseThrow(() -> new RuntimeException("USER_ADMIN role not found")));
            admin.setRoles(roles);

            userRepository.save(admin);
            System.out.println("✅ Admin user created: username=admin, password=admin123");
        }
    }
}