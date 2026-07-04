package com.sms.studentmanagementbackend.controller;

import com.sms.studentmanagementbackend.dto.PageResponse;
import com.sms.studentmanagementbackend.dto.audit.AuditLogResponse;
import java.time.LocalDate;
import com.sms.studentmanagementbackend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
       return ResponseEntity.ok(auditLogService.getAuditLogs(
               q, username, action, entityName, fromDate, toDate, page, size, sortBy, sortDir
       ));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InputStreamResource> exportAuditLogsToExcel(
           @RequestParam(required = false) String q,
           @RequestParam(required = false) String username,
           @RequestParam(required = false) String action,
           @RequestParam(required = false) String entityName,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
           @RequestParam(defaultValue = "timestamp") String sortBy,
           @RequestParam(defaultValue = "desc") String sortDir
    ) {
       return ResponseEntity.ok()
               .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.xlsx")
               .contentType(MediaType.APPLICATION_OCTET_STREAM)
               .body(new InputStreamResource(auditLogService.exportAuditLogsToExcel(
                       q, username, action, entityName, fromDate, toDate, sortBy, sortDir
               )));
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InputStreamResource> exportAuditLogsToPdf(
           @RequestParam(required = false) String q,
           @RequestParam(required = false) String username,
           @RequestParam(required = false) String action,
           @RequestParam(required = false) String entityName,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
           @RequestParam(defaultValue = "timestamp") String sortBy,
           @RequestParam(defaultValue = "desc") String sortDir
    ) {
       return ResponseEntity.ok()
               .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.pdf")
               .contentType(MediaType.APPLICATION_PDF)
               .body(new InputStreamResource(auditLogService.exportAuditLogsToPdf(
                       q, username, action, entityName, fromDate, toDate, sortBy, sortDir
               )));
    }
}
