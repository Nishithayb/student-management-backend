package com.sms.studentmanagementbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 30, columnDefinition = "varchar(30) default 'SYSTEM'")
    private String role;

    @Column(nullable = false, length = 150)
    private String action;

    @Column(nullable = false, length = 100)
    private String entityName;

    @Column(length = 100)
    private String entityId;

    @Column(nullable = false, length = 500, columnDefinition = "varchar(500) default 'No description provided'")
    private String description;

    @Column(nullable = false, length = 100, columnDefinition = "varchar(100) default '127.0.0.1'")
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
