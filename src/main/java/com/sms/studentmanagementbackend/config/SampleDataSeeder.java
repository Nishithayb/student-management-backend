package com.sms.studentmanagementbackend.config;

import com.sms.studentmanagementbackend.entity.AppUser;
import com.sms.studentmanagementbackend.entity.Attendance;
import com.sms.studentmanagementbackend.entity.AuditLog;
import com.sms.studentmanagementbackend.entity.Course;
import com.sms.studentmanagementbackend.entity.Faculty;
import com.sms.studentmanagementbackend.entity.Role;
import com.sms.studentmanagementbackend.entity.Student;
import com.sms.studentmanagementbackend.entity.enums.AttendanceStatus;
import com.sms.studentmanagementbackend.entity.enums.FacultyStatus;
import com.sms.studentmanagementbackend.entity.enums.Gender;
import com.sms.studentmanagementbackend.entity.enums.StudentStatus;
import com.sms.studentmanagementbackend.entity.enums.UserRole;
import com.sms.studentmanagementbackend.repository.AppUserRepository;
import com.sms.studentmanagementbackend.repository.AttendanceRepository;
import com.sms.studentmanagementbackend.repository.AuditLogRepository;
import com.sms.studentmanagementbackend.repository.CourseRepository;
import com.sms.studentmanagementbackend.repository.FacultyRepository;
import com.sms.studentmanagementbackend.repository.RoleRepository;
import com.sms.studentmanagementbackend.repository.StudentRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class SampleDataSeeder implements CommandLineRunner {

    private static final int TARGET_ADMIN_USERS = 2;
    private static final int TARGET_FACULTY_USERS = 5;
    private static final int TARGET_FACULTY_RECORDS = 10;
    private static final int TARGET_STUDENT_RECORDS = 50;
    private static final int TARGET_COURSE_RECORDS = 10;
    private static final int TARGET_ATTENDANCE_RECORDS = 500;
    private static final int TARGET_AUDIT_LOG_RECORDS = 100;

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedUsers();
        seedFaculty();
        seedStudents();
        seedCourses();
        seedRelationships();
        seedAttendance();
        seedAuditLogs();
    }

    private void seedRoles() {
        ensureRole(UserRole.ADMIN, "System administrators with full platform access");
        ensureRole(UserRole.FACULTY, "Faculty members with academic operations access");
    }

    private void ensureRole(UserRole name, String description) {
        roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }

    private void seedUsers() {
        List<UserSeed> admins = List.of(
                new UserSeed("admin", "System Administrator", "admin@sms.local", "9000001001", "FAC9001", "Business Administration", "Admin@123", UserRole.ADMIN),
                new UserSeed("admin2", "Academic Administrator", "admin2@sms.local", "9000001002", "FAC9002", "Business Administration", "Admin@123", UserRole.ADMIN)
        );
        List<UserSeed> facultyUsers = List.of(
                new UserSeed("aarav.sharma", "Aarav Sharma", "aarav.sharma@sms.local", "9000002001", "FAC0001", "Computer Science", "Faculty@123", UserRole.FACULTY),
                new UserSeed("ishita.reddy", "Ishita Reddy", "ishita.reddy@sms.local", "9000002002", "FAC0002", "Information Technology", "Faculty@123", UserRole.FACULTY),
                new UserSeed("rahul.mehta", "Rahul Mehta", "rahul.mehta@sms.local", "9000002003", "FAC0003", "Electronics", "Faculty@123", UserRole.FACULTY),
                new UserSeed("neha.kapoor", "Neha Kapoor", "neha.kapoor@sms.local", "9000002004", "FAC0004", "Mechanical", "Faculty@123", UserRole.FACULTY),
                new UserSeed("kiran.rao", "Kiran Rao", "kiran.rao@sms.local", "9000002005", "FAC0005", "Civil", "Faculty@123", UserRole.FACULTY)
        );

        admins.stream().limit(TARGET_ADMIN_USERS).forEach(this::upsertUser);
        migrateLegacyFacultyUsers(facultyUsers);
        facultyUsers.stream().limit(TARGET_FACULTY_USERS).forEach(this::upsertUser);
    }

    private void migrateLegacyFacultyUsers(List<UserSeed> facultyUsers) {
        List<String> legacyUsernames = List.of("faculty", "faculty2", "faculty3", "faculty4", "faculty5");

        for (int index = 0; index < Math.min(legacyUsernames.size(), facultyUsers.size()); index++) {
            String legacyUsername = legacyUsernames.get(index);
            UserSeed targetSeed = facultyUsers.get(index);
            Optional<AppUser> legacyUserOptional = appUserRepository.findByUsername(legacyUsername);

            if (legacyUserOptional.isEmpty()) {
                continue;
            }

            AppUser legacyUser = legacyUserOptional.get();
            Optional<AppUser> targetUserOptional = appUserRepository.findByUsername(targetSeed.username());
            if (targetUserOptional.isPresent() && !targetUserOptional.get().getId().equals(legacyUser.getId())) {
                appUserRepository.delete(legacyUser);
                continue;
            }

            legacyUser.setUsername(targetSeed.username());
            legacyUser.setFullName(targetSeed.fullName());
            legacyUser.setEmail(targetSeed.email());
            legacyUser.setPhone(targetSeed.phone());
            legacyUser.setEmployeeId(targetSeed.employeeId());
            legacyUser.setDepartment(targetSeed.department());
            legacyUser.setPassword(passwordEncoder.encode(targetSeed.password()));
            legacyUser.setActive(true);
            legacyUser.setRole(roleRepository.findByName(targetSeed.role())
                    .orElseThrow(() -> new IllegalStateException("Role not found: " + targetSeed.role())));
            appUserRepository.save(legacyUser);
        }
    }

    private void upsertUser(UserSeed seed) {
        Optional<AppUser> userOptional = appUserRepository.findByUsername(seed.username());
        if (userOptional.isEmpty()) {
            userOptional = appUserRepository.findByEmail(seed.email());
        }

        AppUser user = userOptional.orElseGet(AppUser::new);
        Role resolvedRole = roleRepository.findByName(seed.role())
                .orElseThrow(() -> new IllegalStateException("Role not found: " + seed.role()));
        user.setUsername(seed.username());
        user.setFullName(seed.fullName());
        user.setEmail(seed.email());
        user.setPhone(seed.phone());
        user.setEmployeeId(seed.employeeId());
        user.setDepartment(seed.department());
        user.setPassword(passwordEncoder.encode(seed.password()));
        user.setRole(resolvedRole);
        user.setActive(true);
        appUserRepository.save(user);
    }

    private void seedFaculty() {
        long existing = facultyRepository.count();
        if (existing >= TARGET_FACULTY_RECORDS) {
            return;
        }

        String[] departments = {"Computer Science", "Electronics", "Mechanical", "Civil", "Information Technology"};
        String[] firstNames = {"Aarav", "Ishita", "Rahul", "Neha", "Kiran", "Priya", "Sanjay", "Divya", "Rohan", "Meera"};
        String[] lastNames = {"Sharma", "Reddy", "Mehta", "Kapoor", "Rao", "Nair", "Verma", "Iyer", "Das", "Joshi"};
        String[] qualifications = {"M.Tech", "Ph.D", "M.E", "Ph.D", "M.Tech", "MBA", "Ph.D", "M.Tech", "M.E", "Ph.D"};
        String[] designations = {"Professor", "Associate Professor", "Assistant Professor", "Assistant Professor", "Lecturer"};

        List<Faculty> records = new ArrayList<>();
        for (int index = (int) existing; index < TARGET_FACULTY_RECORDS; index++) {
            Faculty faculty = new Faculty();
            faculty.setFacultyId(String.format("FAC%03d", index + 1));
            faculty.setName(firstNames[index % firstNames.length] + " " + lastNames[index % lastNames.length]);
            faculty.setEmail("faculty" + (index + 1) + "@campus.local");
            faculty.setPhone("900000" + String.format("%04d", index + 1));
            faculty.setDepartment(departments[index % departments.length]);
            faculty.setQualification(qualifications[index % qualifications.length]);
            faculty.setDesignation(designations[index % designations.length]);
            faculty.setStatus(FacultyStatus.ACTIVE);
            records.add(faculty);
        }
        facultyRepository.saveAll(records);
    }

    private void seedStudents() {
        long existing = studentRepository.count();
        if (existing >= TARGET_STUDENT_RECORDS) {
            return;
        }

        String[] departments = {"Computer Science", "Electronics", "Mechanical", "Civil", "Information Technology"};
        String[] firstNames = {
                "Arjun", "Saanvi", "Vivaan", "Anaya", "Aditya", "Diya", "Krishna", "Anika", "Vihaan", "Sara",
                "Riya", "Kabir", "Aanya", "Reyansh", "Ira", "Aryan", "Myra", "Dhruv", "Nisha", "Tara"
        };
        String[] lastNames = {
                "Patel", "Rao", "Gupta", "Singh", "Kumar", "Menon", "Yadav", "Shah", "Mishra", "Pillai",
                "Reddy", "Malhotra", "Saxena", "Kulkarni", "Bose", "Chopra", "Sethi", "Bhat", "Ghosh", "Jain"
        };

        List<Student> records = new ArrayList<>();
        for (int index = (int) existing; index < TARGET_STUDENT_RECORDS; index++) {
            Student student = new Student();
            student.setStudentId(String.format("STU%03d", index + 1));
            student.setFirstName(firstNames[index % firstNames.length]);
            student.setLastName(lastNames[index % lastNames.length]);
            student.setEmail("student" + (index + 1) + "@campus.local");
            student.setPhone("800000" + String.format("%04d", index + 1));
            student.setDob(LocalDate.of(2001 + (index % 4), ((index % 12) + 1), ((index % 27) + 1)));
            student.setGender(index % 3 == 0 ? Gender.FEMALE : (index % 3 == 1 ? Gender.MALE : Gender.OTHER));
            student.setAddress((index + 10) + ", University Avenue, Academic City");
            student.setDepartment(departments[index % departments.length]);
            student.setSemester((index % 8) + 1);
            student.setJoiningDate(LocalDate.now().minusMonths((index % 24) + 1L));
            student.setStatus(index % 9 == 0 ? StudentStatus.INACTIVE : StudentStatus.ACTIVE);
            student.setImageUrl("https://example.com/student-" + (index + 1) + ".png");
            records.add(student);
        }
        studentRepository.saveAll(records);
    }

    private void seedCourses() {
        long existing = courseRepository.count();
        if (existing >= TARGET_COURSE_RECORDS) {
            return;
        }

        String[] departments = {"Computer Science", "Electronics", "Mechanical", "Civil", "Information Technology"};
        String[] courseNames = {
                "Programming Fundamentals",
                "Data Structures",
                "Circuit Analysis",
                "Thermodynamics",
                "Structural Design",
                "Database Systems",
                "Signals and Systems",
                "Manufacturing Processes",
                "Transportation Engineering",
                "Cloud Computing"
        };

        List<Course> records = new ArrayList<>();
        for (int index = (int) existing; index < TARGET_COURSE_RECORDS; index++) {
            Course course = new Course();
            course.setCourseCode(String.format("CRS%03d", index + 1));
            course.setCourseName(courseNames[index % courseNames.length]);
            course.setCredits((index % 3) + 2);
            course.setSemester((index % 8) + 1);
            course.setDepartment(departments[index % departments.length]);
            records.add(course);
        }
        courseRepository.saveAll(records);
    }

    private void seedRelationships() {
        List<Faculty> facultyList = facultyRepository.findAll();
        List<Student> studentList = studentRepository.findAll();
        List<Course> courseList = courseRepository.findAll();

        if (facultyList.isEmpty() || studentList.isEmpty() || courseList.isEmpty()) {
            return;
        }

        for (int index = 0; index < courseList.size(); index++) {
            Course course = courseList.get(index);

            while (course.getFacultyMembers().size() < 2) {
                Faculty faculty = facultyList.get((index + course.getFacultyMembers().size()) % facultyList.size());
                faculty.assignCourse(course, LocalDate.now().minusDays(index));
            }

            for (int studentIndex = index; studentIndex < studentList.size(); studentIndex += 5) {
                course.getStudents().add(studentList.get(studentIndex));
                if (course.getStudents().size() >= 15) {
                    break;
                }
            }
        }

        courseRepository.saveAll(courseList);
    }

    private void seedAttendance() {
        long existing = attendanceRepository.count();
        if (existing >= TARGET_ATTENDANCE_RECORDS) {
            return;
        }

        List<Course> courseList = courseRepository.findAll();
        if (courseList.isEmpty()) {
            return;
        }

        List<Attendance> records = new ArrayList<>();
        int created = 0;
        for (int dayOffset = 0; dayOffset < 40 && created + existing < TARGET_ATTENDANCE_RECORDS; dayOffset++) {
            LocalDate attendanceDate = LocalDate.now().minusDays(dayOffset);
            for (Course course : courseList) {
                Faculty faculty = course.getFacultyMembers().stream().findFirst().orElse(null);
                for (Student student : course.getStudents()) {
                    if (created + existing >= TARGET_ATTENDANCE_RECORDS) {
                        break;
                    }
                    if (attendanceRepository.findByStudentIdAndCourseIdAndAttendanceDate(student.getId(), course.getId(), attendanceDate).isPresent()) {
                        continue;
                    }

                    Attendance attendance = new Attendance();
                    attendance.setCourse(course);
                    attendance.setStudent(student);
                    attendance.setFaculty(faculty);
                    attendance.setAttendanceDate(attendanceDate);
                    attendance.setStatus((created + dayOffset) % 5 == 0 ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT);
                    attendance.setRemarks((created + dayOffset) % 5 == 0 ? "Medical leave approved" : "Present in class");
                    records.add(attendance);
                    created++;
                }
            }
        }

        attendanceRepository.saveAll(records);
    }

    private void seedAuditLogs() {
        long existing = auditLogRepository.count();
        if (existing >= TARGET_AUDIT_LOG_RECORDS) {
            return;
        }

        String[] actions = {"LOGIN", "CREATE", "UPDATE", "DELETE", "LOGOUT"};
        String[] entities = {"AUTH", "STUDENT", "FACULTY", "COURSE", "ATTENDANCE", "USER"};

        List<AuditLog> records = new ArrayList<>();
        for (int index = (int) existing; index < TARGET_AUDIT_LOG_RECORDS; index++) {
            AuditLog auditLog = new AuditLog();
            boolean adminAction = index % 3 == 0;
            auditLog.setUsername(adminAction ? "admin" : "faculty");
            auditLog.setRole(adminAction ? "ADMIN" : "FACULTY");
            auditLog.setAction(actions[index % actions.length]);
            auditLog.setEntityName(entities[index % entities.length]);
            auditLog.setEntityId(String.valueOf(index + 1));
            auditLog.setDescription("Seeded audit log entry " + (index + 1) + " for " + auditLog.getEntityName());
            auditLog.setIpAddress("127.0.0." + ((index % 20) + 1));
            auditLog.setTimestamp(LocalDateTime.now().minusHours(index));
            records.add(auditLog);
        }

        auditLogRepository.saveAll(records);
    }

    private record UserSeed(
            String username,
            String fullName,
            String email,
            String phone,
            String employeeId,
            String department,
            String password,
            UserRole role
    ) {
    }
}
