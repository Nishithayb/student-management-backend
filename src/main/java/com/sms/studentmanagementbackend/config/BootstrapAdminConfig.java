package com.sms.studentmanagementbackend.config;

import com.sms.studentmanagementbackend.entity.AppUser;
import com.sms.studentmanagementbackend.entity.Role;
import com.sms.studentmanagementbackend.entity.enums.UserRole;
import com.sms.studentmanagementbackend.repository.AppUserRepository;
import com.sms.studentmanagementbackend.repository.RoleRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class BootstrapAdminConfig {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner bootstrapAdmin(
            @Value("${app.bootstrap.admin.username}") String username,
            @Value("${app.bootstrap.admin.full-name}") String fullName,
            @Value("${app.bootstrap.admin.email}") String email,
            @Value("${app.bootstrap.admin.phone}") String phone,
            @Value("${app.bootstrap.admin.employee-id}") String employeeId,
            @Value("${app.bootstrap.admin.department}") String department,
            @Value("${app.bootstrap.admin.password}") String password,
            @Value("${app.bootstrap.faculty.username}") String facultyUsername,
            @Value("${app.bootstrap.faculty.full-name}") String facultyFullName,
            @Value("${app.bootstrap.faculty.email}") String facultyEmail,
            @Value("${app.bootstrap.faculty.phone}") String facultyPhone,
            @Value("${app.bootstrap.faculty.employee-id}") String facultyEmployeeId,
            @Value("${app.bootstrap.faculty.department}") String facultyDepartment,
            @Value("${app.bootstrap.faculty.password}") String facultyPassword
    ) {
        return args -> {
            upsertUser(username, fullName, email, phone, employeeId, department, password, UserRole.ADMIN);
            upsertUser(facultyUsername, facultyFullName, facultyEmail, facultyPhone, facultyEmployeeId, facultyDepartment, facultyPassword, UserRole.FACULTY);
        };
    }

    private void upsertUser(
            String username,
            String fullName,
            String email,
            String phone,
            String employeeId,
            String department,
            String rawPassword,
            UserRole role
    ) {
        Optional<AppUser> userOptional = appUserRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            userOptional = appUserRepository.findByEmail(email);
        }

        AppUser user = userOptional.orElseGet(AppUser::new);
        Role resolvedRole = roleRepository.findByName(role)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + role));
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setEmployeeId(employeeId);
        user.setDepartment(department);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(resolvedRole);
        user.setActive(true);
        appUserRepository.save(user);
    }
}
