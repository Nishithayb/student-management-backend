package com.sms.studentmanagementbackend.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.sms.studentmanagementbackend.dto.PageResponse;
import com.sms.studentmanagementbackend.dto.course.CourseSummaryResponse;
import com.sms.studentmanagementbackend.dto.student.StudentImportError;
import com.sms.studentmanagementbackend.dto.student.StudentImportResponse;
import com.sms.studentmanagementbackend.dto.student.StudentRequest;
import com.sms.studentmanagementbackend.dto.student.StudentResponse;
import com.sms.studentmanagementbackend.entity.Course;
import com.sms.studentmanagementbackend.entity.Student;
import com.sms.studentmanagementbackend.entity.enums.AuditAction;
import com.sms.studentmanagementbackend.entity.enums.Gender;
import com.sms.studentmanagementbackend.entity.enums.StudentStatus;
import com.sms.studentmanagementbackend.exception.BadRequestException;
import com.sms.studentmanagementbackend.exception.DuplicateResourceException;
import com.sms.studentmanagementbackend.exception.NotFoundException;
import com.sms.studentmanagementbackend.repository.AttendanceRepository;
import com.sms.studentmanagementbackend.repository.CourseRepository;
import com.sms.studentmanagementbackend.repository.StudentRepository;
import com.sms.studentmanagementbackend.util.ExportUtils;
import com.sms.studentmanagementbackend.util.PageUtils;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\- ]{7,20}$");

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> getStudents(
            String q,
            String department,
            Integer semester,
            StudentStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = PageUtils.createPageable(page, size, sortBy, sortDir);
        Page<StudentResponse> result = studentRepository.findAll(buildSpecification(q, department, semester, status), pageable)
                .map(this::toResponse);
        return PageUtils.toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(Long id) {
        return toResponse(findStudent(id));
    }

    @Transactional
    public StudentResponse createStudent(StudentRequest request) {
        validateStudentRequest(request, null);
        Student student = new Student();
        applyRequest(student, request);
        studentRepository.save(student);
        auditLogService.log(
                AuditAction.CREATE,
                "STUDENT",
                String.valueOf(student.getId()),
                "Created student " + student.getStudentId() + " (" + student.getFirstName() + " " + student.getLastName() + ")"
        );
        return toResponse(student);
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentRequest request) {
        validateStudentRequest(request, id);
        Student student = findStudent(id);
        applyRequest(student, request);
        studentRepository.save(student);
        auditLogService.log(
                AuditAction.UPDATE,
                "STUDENT",
                String.valueOf(student.getId()),
                "Updated student " + student.getStudentId() + " (" + student.getFirstName() + " " + student.getLastName() + ")"
        );
        return toResponse(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = findStudent(id);
        attendanceRepository.deleteByStudentId(id);
        Set<Course> courses = new HashSet<>(student.getCourses());
        for (Course course : courses) {
            course.getStudents().remove(student);
            courseRepository.save(course);
        }
        studentRepository.delete(student);
        auditLogService.log(
                AuditAction.DELETE,
                "STUDENT",
                String.valueOf(id),
                "Deleted student " + student.getStudentId() + " (" + student.getFirstName() + " " + student.getLastName() + ")"
        );
    }

    @Transactional
    public StudentImportResponse importStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            StudentImportResponse response;
            if (filename.endsWith(".csv")) {
                response = importFromCsv(file);
            } else if (filename.endsWith(".xlsx")) {
                response = importFromExcel(file);
            } else {
                throw new BadRequestException("Only CSV and XLSX files are supported");
            }

            auditLogService.log(
                    AuditAction.CREATE,
                    "STUDENT_IMPORT",
                    null,
                    "Imported students from " + filename + ": " + response.importedCount() + " imported, "
                            + response.duplicateCount() + " duplicates, " + response.failedCount() + " failed"
            );
            return response;
        } catch (IOException | CsvValidationException exception) {
            throw new BadRequestException("Failed to import students: " + exception.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportStudentsToExcel(
            String q,
            String department,
            Integer semester,
            StudentStatus status,
            String sortBy,
            String sortDir
    ) {
        List<Student> students = findStudents(q, department, semester, status, sortBy, sortDir);
        auditLogService.log(AuditAction.CREATE, "STUDENT_EXPORT", null, "Exported " + students.size() + " students to Excel");
        return ExportUtils.toExcel("Students", studentHeaders(), toExportRows(students));
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportStudentsToPdf(
            String q,
            String department,
            Integer semester,
            StudentStatus status,
            String sortBy,
            String sortDir
    ) {
        List<Student> students = findStudents(q, department, semester, status, sortBy, sortDir);
        auditLogService.log(AuditAction.CREATE, "STUDENT_EXPORT", null, "Exported " + students.size() + " students to PDF");
        return ExportUtils.toPdf("Students Report", studentHeaders(), toExportRows(students));
    }

    private Specification<Student> buildSpecification(String q, String department, Integer semester, StudentStatus status) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            if (q != null && !q.isBlank()) {
                String term = "%" + q.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("studentId")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("department")), term)
                ));
            }
            if (department != null && !department.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("department")), department.trim().toLowerCase()));
            }
            if (semester != null) {
                predicates.add(criteriaBuilder.equal(root.get("semester"), semester));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private List<Student> findStudents(
            String q,
            String department,
            Integer semester,
            StudentStatus status,
            String sortBy,
            String sortDir
    ) {
        Sort sort = PageUtils.createSort(sortBy, sortDir);
        return studentRepository.findAll(buildSpecification(q, department, semester, status), sort);
    }

    private void validateStudentRequest(StudentRequest request, Long id) {
        if (id == null) {
            if (studentRepository.existsByStudentId(request.studentId())) {
                throw new DuplicateResourceException("Student ID already exists");
            }
            if (studentRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("Student email already exists");
            }
            return;
        }

        if (studentRepository.existsByStudentIdAndIdNot(request.studentId(), id)) {
            throw new DuplicateResourceException("Student ID already exists");
        }
        if (studentRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("Student email already exists");
        }
    }

    private void applyRequest(Student student, StudentRequest request) {
        student.setStudentId(request.studentId());
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());
        student.setPhone(request.phone());
        student.setDob(request.dob());
        student.setGender(request.gender());
        student.setAddress(request.address());
        student.setDepartment(request.department());
        student.setSemester(request.semester());
        student.setJoiningDate(request.joiningDate());
        student.setStatus(request.status());
        student.setImageUrl(request.imageUrl());

        Set<Course> selectedCourses = request.courseIds() == null || request.courseIds().isEmpty()
                ? new HashSet<>()
                : new HashSet<>(courseRepository.findAllById(request.courseIds()));
        if (request.courseIds() != null && selectedCourses.size() != request.courseIds().size()) {
            throw new NotFoundException("One or more courses were not found");
        }

        Set<Course> existingCourses = new HashSet<>(student.getCourses());
        for (Course course : existingCourses) {
            course.getStudents().remove(student);
            courseRepository.save(course);
        }

        student.getCourses().clear();
        for (Course course : selectedCourses) {
            course.getStudents().add(student);
            student.getCourses().add(course);
            courseRepository.save(course);
        }
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new NotFoundException("Student not found"));
    }

    private StudentResponse toResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .studentId(student.getStudentId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .dob(student.getDob())
                .gender(student.getGender())
                .address(student.getAddress())
                .department(student.getDepartment())
                .semester(student.getSemester())
                .joiningDate(student.getJoiningDate())
                .status(student.getStatus())
                .imageUrl(student.getImageUrl())
                .courses(student.getCourses().stream()
                        .map(course -> new CourseSummaryResponse(course.getId(), course.getCourseCode(), course.getCourseName()))
                        .sorted(Comparator.comparing(CourseSummaryResponse::courseCode))
                        .toList())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }

    private StudentImportResponse importFromCsv(MultipartFile file) throws IOException, CsvValidationException {
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            csvReader.readNext();
            List<String[]> rows = new ArrayList<>();
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                rows.add(values);
            }
            return importRows(rows);
        }
    }

    private StudentImportResponse importFromExcel(MultipartFile file) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String[]> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String[] values = new String[13];
                for (int index = 0; index < values.length; index++) {
                    values[index] = row.getCell(index) == null ? "" : row.getCell(index).toString().trim();
                }
                rows.add(values);
            }
            return importRows(rows);
        }
    }

    private StudentImportResponse importRows(List<String[]> rows) {
        List<StudentImportError> errors = new ArrayList<>();
        Set<String> fileStudentIds = new HashSet<>();
        Set<String> fileEmails = new HashSet<>();
        int importedCount = 0;
        int duplicateCount = 0;

        for (int index = 0; index < rows.size(); index++) {
            int rowNumber = index + 2;
            try {
                StudentRequest request = toStudentRequest(rows.get(index));
                String normalizedStudentId = request.studentId().trim().toUpperCase(Locale.ROOT);
                String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

                if (!fileStudentIds.add(normalizedStudentId) || !fileEmails.add(normalizedEmail)) {
                    duplicateCount++;
                    errors.add(StudentImportError.builder()
                            .rowNumber(rowNumber)
                            .studentId(request.studentId())
                            .message("Duplicate student ID or email found in file")
                            .build());
                    continue;
                }
                if (studentRepository.existsByStudentId(request.studentId()) || studentRepository.existsByEmail(request.email())) {
                    duplicateCount++;
                    errors.add(StudentImportError.builder()
                            .rowNumber(rowNumber)
                            .studentId(request.studentId())
                            .message("Student ID or email already exists")
                            .build());
                    continue;
                }

                createStudent(request);
                importedCount++;
            } catch (Exception exception) {
                errors.add(StudentImportError.builder()
                        .rowNumber(rowNumber)
                        .studentId(extractStudentId(rows.get(index)))
                        .message(exception.getMessage())
                        .build());
            }
        }

        return StudentImportResponse.builder()
                .totalRows(rows.size())
                .importedCount(importedCount)
                .failedCount(errors.size())
                .duplicateCount(duplicateCount)
                .errors(errors)
                .build();
    }

    private StudentRequest toStudentRequest(String[] values) {
        if (values.length < 12) {
            throw new BadRequestException("Student import rows must include Student ID, Name, Email, Phone, DOB, Gender, Address, Department, Semester, Joining Date, and Status");
        }

        String studentId = required(values, 0, "Student ID");
        String firstName = required(values, 1, "First name");
        String lastName = required(values, 2, "Last name");
        String email = required(values, 3, "Email");
        String phone = required(values, 4, "Phone");
        String address = required(values, 7, "Address");
        String department = required(values, 8, "Department");

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Invalid email format");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BadRequestException("Invalid phone format");
        }

        return new StudentRequest(
                studentId,
                firstName,
                lastName,
                email,
                phone,
                LocalDate.parse(required(values, 5, "Date of birth")),
                Gender.valueOf(required(values, 6, "Gender").toUpperCase(Locale.ROOT)),
                address,
                department,
                Integer.parseInt(required(values, 9, "Semester")),
                LocalDate.parse(required(values, 10, "Joining date")),
                StudentStatus.valueOf(required(values, 11, "Status").toUpperCase(Locale.ROOT)),
                values.length > 12 && values[12] != null && !values[12].isBlank() ? values[12].trim() : null,
                Set.of()
        );
    }

    private String required(String[] values, int index, String label) {
        if (index >= values.length || values[index] == null || values[index].isBlank()) {
            throw new BadRequestException(label + " is required");
        }
        return values[index].trim();
    }

    private String extractStudentId(String[] values) {
        return values.length > 0 && values[0] != null ? values[0].trim() : "";
    }

    private List<String> studentHeaders() {
        return List.of("Student ID", "Name", "Email", "Phone", "Department", "Semester", "Gender", "DOB", "Status", "Joining Date");
    }

    private List<List<String>> toExportRows(List<Student> students) {
        return students.stream()
                .map(student -> List.of(
                        safe(student.getStudentId()),
                        safe(student.getFirstName() + " " + student.getLastName()),
                        safe(student.getEmail()),
                        safe(student.getPhone()),
                        safe(student.getDepartment()),
                        String.valueOf(student.getSemester()),
                        student.getGender().name(),
                        String.valueOf(student.getDob()),
                        student.getStatus().name(),
                        String.valueOf(student.getJoiningDate())
                ))
                .toList();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
