package com.sms.studentmanagementbackend.repository;

import com.sms.studentmanagementbackend.entity.Role;
import com.sms.studentmanagementbackend.entity.enums.UserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(UserRole name);
}
