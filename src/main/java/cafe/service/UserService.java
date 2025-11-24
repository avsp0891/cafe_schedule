package cafe.service;

import cafe.dto.UserDto;
import cafe.model.Role;
import cafe.model.User;
import cafe.repository.RoleRepository;
import cafe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(UserDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        Set<Role> roles = new HashSet<>();
        if (dto.getRoles() == null || dto.getRoles().isEmpty()) {
            roles.add(roleRepository.findByName(Role.ERole.STAFF)
                    .orElseThrow(() -> new RuntimeException("Default role not found")));
        } else {
            for (String roleName : dto.getRoles()) {
                Role.ERole role = Role.ERole.valueOf(roleName);
                roles.add(roleRepository.findByName(role)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)));
            }
        }
        user.setRoles(roles);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, UserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new RuntimeException("Username is already taken!");
            }
            user.setUsername(dto.getUsername());
        }

        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("Email is already in use!");
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            Set<Role> roles = dto.getRoles().stream()
                    .map(roleName -> roleRepository.findByName(Role.ERole.valueOf(roleName))
                            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
