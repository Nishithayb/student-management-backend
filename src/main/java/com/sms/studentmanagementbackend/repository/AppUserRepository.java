package com.sms.studentmanagementbackend.repository;

import com.sms.studentmanagementbackend.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmployeeId(String employeeId);

    @Query("select count(u) > 0 from AppUser u join u.roles r where r.name = :role")
    boolean existsByRoleName(com.sms.studentmanagementbackend.entity.enums.UserRole role);
}
