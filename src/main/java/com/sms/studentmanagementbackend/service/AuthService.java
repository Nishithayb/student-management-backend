package com.sms.studentmanagementbackend.service;

import com.sms.studentmanagementbackend.dto.auth.AuthResponse;
import com.sms.studentmanagementbackend.dto.auth.LoginRequest;
import com.sms.studentmanagementbackend.dto.auth.RegistrationResponse;
import com.sms.studentmanagementbackend.dto.auth.RefreshTokenRequest;
import com.sms.studentmanagementbackend.dto.auth.RegisterRequest;
import com.sms.studentmanagementbackend.dto.auth.UserProfileResponse;
import com.sms.studentmanagementbackend.entity.AppUser;
import com.sms.studentmanagementbackend.entity.RefreshToken;
import com.sms.studentmanagementbackend.entity.Role;
import com.sms.studentmanagementbackend.entity.enums.AuditAction;
import com.sms.studentmanagementbackend.entity.enums.UserRole;
import com.sms.studentmanagementbackend.exception.BadRequestException;
import com.sms.studentmanagementbackend.exception.DuplicateResourceException;
import com.sms.studentmanagementbackend.exception.NotFoundException;
import com.sms.studentmanagementbackend.repository.AppUserRepository;
import com.sms.studentmanagementbackend.repository.RoleRepository;
import com.sms.studentmanagementbackend.security.CustomUserDetails;
import com.sms.studentmanagementbackend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password())
        );

        AppUser user = appUserRepository.findByUsernameOrEmail(request.usernameOrEmail(), request.usernameOrEmail())
                .orElseThrow(() -> new NotFoundException("User not found"));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.issueToken(user);
        auditLogService.log(
                user.getUsername(),
                user.getRole().name(),
                AuditAction.LOGIN,
                "AUTH",
                String.valueOf(user.getId()),
                "User logged in",
                auditLogService.currentIpAddress()
        );
        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        if (request.role() != UserRole.FACULTY) {
            throw new BadRequestException("Only faculty accounts can be created from the public registration form");
        }
        return createUser(request, UserRole.FACULTY, true);
    }

    @Transactional
    public RegistrationResponse registerByAdmin(RegisterRequest request) {
        return createUser(request, request.role(), false);
    }

    @Transactional
    private RegistrationResponse createUser(RegisterRequest request, UserRole resolvedRoleName, boolean selfRegistration) {
        if (appUserRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (appUserRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists");
        }
        if (appUserRepository.existsByEmployeeId(request.employeeId())) {
            throw new DuplicateResourceException("Employee ID already exists");
        }

        AppUser user = new AppUser();
        Role resolvedRole = roleRepository.findByName(resolvedRoleName)
                .orElseThrow(() -> new NotFoundException("Role not found: " + resolvedRoleName));
        user.setUsername(request.username().trim());
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim());
        user.setPhone(request.phone().trim());
        user.setEmployeeId(request.employeeId().trim());
        user.setDepartment(request.department().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(resolvedRole);
        user.setActive(true);
        appUserRepository.save(user);

        auditLogService.log(
                selfRegistration ? user.getUsername() : auditLogService.currentUsername(),
                selfRegistration ? user.getRole().name() : auditLogService.currentRole(),
                AuditAction.CREATE,
                "USER",
                String.valueOf(user.getId()),
                selfRegistration
                        ? "Registered faculty account " + user.getUsername()
                        : "Created user " + user.getUsername() + " with role " + user.getRole().name(),
                auditLogService.currentIpAddress()
        );
        return RegistrationResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .employeeId(user.getEmployeeId())
                .department(user.getDepartment())
                .role(user.getRole())
                .message("Account created successfully.")
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verify(request.refreshToken());
        AppUser user = refreshToken.getUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken newRefreshToken = refreshTokenService.issueToken(user);
        refreshTokenService.revoke(request.refreshToken());
        return buildAuthResponse(user, accessToken, newRefreshToken.getToken());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verify(request.refreshToken());
        refreshTokenService.revoke(request.refreshToken());
        auditLogService.log(
                refreshToken.getUser().getUsername(),
                refreshToken.getUser().getRole().name(),
                AuditAction.LOGOUT,
                "AUTH",
                String.valueOf(refreshToken.getUser().getId()),
                "User logged out",
                auditLogService.currentIpAddress()
        );
    }

    public UserProfileResponse profile(CustomUserDetails userDetails) {
        AppUser user = appUserRepository.findById(userDetails.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .employeeId(user.getEmployeeId())
                .department(user.getDepartment())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthResponse buildAuthResponse(AppUser user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
