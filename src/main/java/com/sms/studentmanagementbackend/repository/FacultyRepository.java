package com.sms.studentmanagementbackend.repository;

import com.sms.studentmanagementbackend.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FacultyRepository extends JpaRepository<Faculty, Long>, JpaSpecificationExecutor<Faculty> {

    boolean existsByFacultyId(String facultyId);

    boolean existsByFacultyIdAndIdNot(String facultyId, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
