package com.sms.studentmanagementbackend.entity;

import com.sms.studentmanagementbackend.entity.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "employeeId")
        }
)
public class AppUser extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 150, columnDefinition = "varchar(150) default 'System User'")
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default '0000000000'")
    private String phone;

    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'FAC0000'")
    private String employeeId;

    @Column(nullable = false, length = 100, columnDefinition = "varchar(100) default 'Computer Science'")
    private String department;

    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    public UserRole getRole() {
        return roles.stream()
                .map(Role::getName)
                .findFirst()
                .orElse(UserRole.FACULTY);
    }

    public void setRole(Role role) {
        roles.clear();
        if (role != null) {
            roles.add(role);
        }
    }
}
