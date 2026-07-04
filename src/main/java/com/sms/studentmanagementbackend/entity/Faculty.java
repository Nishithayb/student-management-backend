package com.sms.studentmanagementbackend.entity;

import com.sms.studentmanagementbackend.entity.enums.FacultyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.CascadeType;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "faculty",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "facultyId"),
                @UniqueConstraint(columnNames = "email")
        }
)
public class Faculty extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String facultyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 150)
    private String qualification;

    @Column(nullable = false, length = 120, columnDefinition = "varchar(120) default 'Lecturer'")
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'ACTIVE'")
    private FacultyStatus status;

    @OneToMany(mappedBy = "faculty")
    private Set<FacultyCourse> facultyCourses = new HashSet<>();

    public Set<Course> getCourses() {
        return facultyCourses.stream()
                .map(FacultyCourse::getCourse)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    public void assignCourse(Course course, LocalDate assignedDate) {
        boolean exists = facultyCourses.stream().anyMatch(item -> item.getCourse().equals(course));
        if (exists) {
            return;
        }
        FacultyCourse facultyCourse = new FacultyCourse();
        facultyCourse.setFaculty(this);
        facultyCourse.setCourse(course);
        facultyCourse.setAssignedDate(assignedDate);
        facultyCourses.add(facultyCourse);
        course.getFacultyAssignments().add(facultyCourse);
    }

    public void removeCourse(Course course) {
        facultyCourses.removeIf(item -> {
            boolean match = item.getCourse().equals(course);
            if (match) {
                course.getFacultyAssignments().remove(item);
            }
            return match;
        });
    }
}
