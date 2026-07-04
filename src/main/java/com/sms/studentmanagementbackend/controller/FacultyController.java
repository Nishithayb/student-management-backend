package com.sms.studentmanagementbackend.controller;

import com.sms.studentmanagementbackend.dto.PageResponse;
import com.sms.studentmanagementbackend.dto.course.CourseSummaryResponse;
import com.sms.studentmanagementbackend.dto.faculty.FacultyRequest;
import com.sms.studentmanagementbackend.dto.faculty.FacultyResponse;
import com.sms.studentmanagementbackend.entity.enums.FacultyStatus;
import com.sms.studentmanagementbackend.service.FacultyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
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
@RequestMapping("/api/faculty")
@RequiredArgsConstructor
public class FacultyController {

    private final FacultyService facultyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<PageResponse<FacultyResponse>> getFaculty(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) FacultyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(facultyService.getFaculty(q, department, status, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<FacultyResponse> getFacultyById(@PathVariable Long id) {
        return ResponseEntity.ok(facultyService.getFacultyById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacultyResponse> createFaculty(@Valid @RequestBody FacultyRequest request) {
        return ResponseEntity.status(201).body(facultyService.createFaculty(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacultyResponse> updateFaculty(@PathVariable Long id, @Valid @RequestBody FacultyRequest request) {
        return ResponseEntity.ok(facultyService.updateFaculty(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFaculty(@PathVariable Long id) {
        facultyService.deleteFaculty(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{facultyId}/courses/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacultyResponse> assignCourse(@PathVariable Long facultyId, @PathVariable Long courseId) {
        return ResponseEntity.ok(facultyService.assignCourse(facultyId, courseId));
    }

    @DeleteMapping("/{facultyId}/courses/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacultyResponse> removeCourseAssignment(@PathVariable Long facultyId, @PathVariable Long courseId) {
        return ResponseEntity.ok(facultyService.removeCourseAssignment(facultyId, courseId));
    }

    @GetMapping("/{facultyId}/courses")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<List<CourseSummaryResponse>> getAssignedCourses(@PathVariable Long facultyId) {
        return ResponseEntity.ok(facultyService.getAssignedCourses(facultyId));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<InputStreamResource> exportFacultyToExcel(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) FacultyStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=faculty.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(facultyService.exportFacultyToExcel(q, department, status, sortBy, sortDir)));
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<InputStreamResource> exportFacultyToPdf(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) FacultyStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=faculty.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(facultyService.exportFacultyToPdf(q, department, status, sortBy, sortDir)));
    }
}
