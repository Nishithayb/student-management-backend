package com.sms.studentmanagementbackend.service;

import com.sms.studentmanagementbackend.dto.PageResponse;
import com.sms.studentmanagementbackend.dto.audit.AuditLogResponse;
import com.sms.studentmanagementbackend.entity.AuditLog;
import com.sms.studentmanagementbackend.entity.enums.AuditAction;
import com.sms.studentmanagementbackend.repository.AuditLogRepository;
import com.sms.studentmanagementbackend.util.ExportUtils;
import com.sms.studentmanagementbackend.util.PageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String username, String role, AuditAction action, String entityName, String entityId, String description, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(username);
        auditLog.setRole(role);
        auditLog.setAction(action.name());
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setDescription(description);
        auditLog.setIpAddress(ipAddress);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    @Transactional
    public void log(AuditAction action, String entityName, String entityId, String description) {
        log(currentUsername(), currentRole(), action, entityName, entityId, description, currentIpAddress());
    }

    public String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    public String currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            return "SYSTEM";
        }
        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        return authority.startsWith("ROLE_") ? authority.substring(5) : authority;
    }

    public String currentIpAddress() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return "127.0.0.1";
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(
            String q,
            String username,
            String action,
            String entityName,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = PageUtils.createPageable(page, size, sortBy, sortDir);
        Specification<AuditLog> specification = buildSpecification(q, username, action, entityName, fromDate, toDate);
        Page<AuditLogResponse> result = auditLogRepository.findAll(specification, pageable)
                .map(this::toResponse);
        return PageUtils.toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportAuditLogsToExcel(
            String q,
            String username,
            String action,
            String entityName,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    ) {
        List<AuditLog> logs = findAuditLogs(q, username, action, entityName, fromDate, toDate, sortBy, sortDir);
        return ExportUtils.toExcel("Audit Logs", auditHeaders(), toRows(logs));
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportAuditLogsToPdf(
            String q,
            String username,
            String action,
            String entityName,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    ) {
        List<AuditLog> logs = findAuditLogs(q, username, action, entityName, fromDate, toDate, sortBy, sortDir);
        return ExportUtils.toPdf("Audit Logs Report", auditHeaders(), toRows(logs));
    }

    private Specification<AuditLog> buildSpecification(
            String q,
            String username,
            String action,
            String entityName,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Specification<AuditLog> specification = (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            if (q != null && !q.isBlank()) {
                String term = "%" + q.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("role")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("action")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("entityName")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("entityId")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ipAddress")), term)
                ));
            }
            if (username != null && !username.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("username")), username.trim().toLowerCase()));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("action")), action.trim().toLowerCase()));
            }
            if (entityName != null && !entityName.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("entityName")), entityName.trim().toLowerCase()));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), toDate.plusDays(1).atStartOfDay().minusNanos(1)));
            }
            return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return specification;
    }

    private List<AuditLog> findAuditLogs(
            String q,
            String username,
            String action,
            String entityName,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    ) {
        Specification<AuditLog> specification = buildSpecification(q, username, action, entityName, fromDate, toDate);
        Sort sort = PageUtils.createSort(sortBy, sortDir);
        return auditLogRepository.findAll(specification, sort);
    }

    private List<String> auditHeaders() {
        return List.of("Timestamp", "Username", "Role", "Action", "Entity", "Entity ID", "Description", "IP Address");
    }

    private List<List<String>> toRows(List<AuditLog> logs) {
        return logs.stream()
                .map(log -> List.of(
                        String.valueOf(log.getTimestamp()),
                        defaultValue(log.getUsername()),
                        defaultValue(log.getRole()),
                        defaultValue(log.getAction()),
                        defaultValue(log.getEntityName()),
                        defaultValue(log.getEntityId()),
                        defaultValue(log.getDescription()),
                        defaultValue(log.getIpAddress())
                ))
                .toList();
    }

    private String defaultValue(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .username(auditLog.getUsername())
                .role(auditLog.getRole())
                .action(auditLog.getAction())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .description(auditLog.getDescription())
                .ipAddress(auditLog.getIpAddress())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}
