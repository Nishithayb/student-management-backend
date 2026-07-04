package com.sms.studentmanagementbackend.controller;

import com.sms.studentmanagementbackend.dto.PageResponse;
import com.sms.studentmanagementbackend.dto.attendance.AttendanceMonthlyReportResponse;
import com.sms.studentmanagementbackend.dto.attendance.AttendanceReportResponse;
import com.sms.studentmanagementbackend.dto.attendance.AttendanceRequest;
import com.sms.studentmanagementbackend.dto.attendance.AttendanceResponse;
import com.sms.studentmanagementbackend.entity.enums.AttendanceStatus;
import com.sms.studentmanagementbackend.service.AttendanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<AttendanceResponse> markAttendance(@Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.status(201).body(attendanceService.markAttendance(request));
    }

    @PutMapping("/{attendanceId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<AttendanceResponse> updateAttendance(
            @PathVariable Long attendanceId,
            @Valid @RequestBody AttendanceRequest request
    ) {
        return ResponseEntity.ok(attendanceService.updateAttendance(attendanceId, request));
    }

    @DeleteMapping("/{attendanceId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long attendanceId) {
        attendanceService.deleteAttendance(attendanceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<PageResponse<AttendanceResponse>> getAttendance(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "attendanceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(attendanceService.getAttendance(
                studentId, courseId, facultyId, status, fromDate, toDate, page, size, sortBy, sortDir
        ));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<PageResponse<AttendanceResponse>> getAttendanceByStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "attendanceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(attendanceService.getAttendanceByStudent(studentId, fromDate, toDate, page, size, sortBy, sortDir));
    }

    @GetMapping("/student/{studentId}/report")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<AttendanceReportResponse> getStudentReport(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(attendanceService.getStudentReport(studentId, fromDate, toDate));
    }

    @GetMapping("/course/{courseId}/report")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<AttendanceReportResponse> getCourseReport(
            @PathVariable Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(attendanceService.getCourseReport(courseId, fromDate, toDate));
    }

    @GetMapping("/reports/monthly")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<AttendanceMonthlyReportResponse> getMonthlyReport(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(required = false) Long courseId
    ) {
        return ResponseEntity.ok(attendanceService.getMonthlyReport(month, year, courseId));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<InputStreamResource> exportAttendanceToExcel(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "attendanceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(attendanceService.exportAttendanceToExcel(
                        studentId, courseId, facultyId, status, fromDate, toDate, sortBy, sortDir
                )));
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<InputStreamResource> exportAttendanceToPdf(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "attendanceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(attendanceService.exportAttendanceToPdf(
                        studentId, courseId, facultyId, status, fromDate, toDate, sortBy, sortDir
                )));
    }
}
