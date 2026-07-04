package com.sms.studentmanagementbackend.repository;

import com.sms.studentmanagementbackend.entity.FacultyCourse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyCourseRepository extends JpaRepository<FacultyCourse, Long> {

    boolean existsByFacultyIdAndCourseId(Long facultyId, Long courseId);

    Optional<FacultyCourse> findByFacultyIdAndCourseId(Long facultyId, Long courseId);
}
