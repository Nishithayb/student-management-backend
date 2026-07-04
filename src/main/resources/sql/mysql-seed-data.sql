-- Student Management System sample seed for MySQL 8
-- Assumes the application schema already exists.

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM attendance;
DELETE FROM audit_logs;
DELETE FROM faculty_courses;
DELETE FROM student_courses;
DELETE FROM refresh_tokens;
DELETE FROM user_roles;
DELETE FROM roles;
DELETE FROM students;
DELETE FROM faculty;
DELETE FROM courses;
DELETE FROM users;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO roles (name, description) VALUES
('ADMIN', 'System administrators with full platform access'),
('FACULTY', 'Faculty members with academic operations access');

INSERT INTO users (username, full_name, email, phone, employee_id, department, password, active, created_at, updated_at) VALUES
('admin', 'System Administrator', 'admin@sms.local', '9000001001', 'FAC9001', 'Business Administration', '$2a$10$jRiQRnILKTs1fTn6qitsXek8.p7LMIkx5/.AWSi1xAzP2Fx3fjwLO', 1, NOW(), NOW()),
('admin2', 'Academic Administrator', 'admin2@sms.local', '9000001002', 'FAC9002', 'Business Administration', '$2a$10$jRiQRnILKTs1fTn6qitsXek8.p7LMIkx5/.AWSi1xAzP2Fx3fjwLO', 1, NOW(), NOW()),
('aarav.sharma', 'Aarav Sharma', 'aarav.sharma@sms.local', '9000002001', 'FAC0001', 'Computer Science', '$2a$10$y/Jt44UK1exWdQFWTPZV9uOb2iWf/ZPwMCu7sHEAl.igLHMIBnoy6', 1, NOW(), NOW()),
('ishita.reddy', 'Ishita Reddy', 'ishita.reddy@sms.local', '9000002002', 'FAC0002', 'Information Technology', '$2a$10$y/Jt44UK1exWdQFWTPZV9uOb2iWf/ZPwMCu7sHEAl.igLHMIBnoy6', 1, NOW(), NOW()),
('rahul.mehta', 'Rahul Mehta', 'rahul.mehta@sms.local', '9000002003', 'FAC0003', 'Electronics', '$2a$10$y/Jt44UK1exWdQFWTPZV9uOb2iWf/ZPwMCu7sHEAl.igLHMIBnoy6', 1, NOW(), NOW()),
('neha.kapoor', 'Neha Kapoor', 'neha.kapoor@sms.local', '9000002004', 'FAC0004', 'Mechanical', '$2a$10$y/Jt44UK1exWdQFWTPZV9uOb2iWf/ZPwMCu7sHEAl.igLHMIBnoy6', 1, NOW(), NOW()),
('kiran.rao', 'Kiran Rao', 'kiran.rao@sms.local', '9000002005', 'FAC0005', 'Civil', '$2a$10$y/Jt44UK1exWdQFWTPZV9uOb2iWf/ZPwMCu7sHEAl.igLHMIBnoy6', 1, NOW(), NOW());

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = CASE WHEN u.username LIKE 'admin%' THEN 'ADMIN' ELSE 'FACULTY' END;

WITH RECURSIVE faculty_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM faculty_seq WHERE n < 10
)
INSERT INTO faculty (faculty_id, name, email, phone, department, qualification, designation, status, created_at, updated_at)
SELECT
    CONCAT('FAC', LPAD(n, 3, '0')),
    CASE n
        WHEN 1 THEN 'Aarav Sharma'
        WHEN 2 THEN 'Ishita Reddy'
        WHEN 3 THEN 'Rahul Mehta'
        WHEN 4 THEN 'Neha Kapoor'
        WHEN 5 THEN 'Kiran Rao'
        WHEN 6 THEN 'Priya Nair'
        WHEN 7 THEN 'Sanjay Verma'
        WHEN 8 THEN 'Divya Iyer'
        WHEN 9 THEN 'Rohan Das'
        ELSE 'Meera Joshi'
    END,
    CONCAT('faculty', n, '@campus.local'),
    CONCAT('900000', LPAD(n, 4, '0')),
    ELT(((n - 1) MOD 5) + 1, 'Computer Science', 'Electronics', 'Mechanical', 'Civil', 'Information Technology'),
    ELT(((n - 1) MOD 5) + 1, 'M.Tech', 'Ph.D', 'M.E', 'Ph.D', 'MBA'),
    ELT(((n - 1) MOD 5) + 1, 'Professor', 'Associate Professor', 'Assistant Professor', 'Assistant Professor', 'Lecturer'),
    'ACTIVE',
    NOW(),
    NOW()
FROM faculty_seq;

INSERT INTO courses (course_code, course_name, credits, semester, department, created_at, updated_at) VALUES
('CRS001', 'Programming Fundamentals', 2, 1, 'Computer Science', NOW(), NOW()),
('CRS002', 'Data Structures', 3, 2, 'Electronics', NOW(), NOW()),
('CRS003', 'Circuit Analysis', 4, 3, 'Mechanical', NOW(), NOW()),
('CRS004', 'Thermodynamics', 2, 4, 'Civil', NOW(), NOW()),
('CRS005', 'Structural Design', 3, 5, 'Information Technology', NOW(), NOW()),
('CRS006', 'Database Systems', 4, 6, 'Computer Science', NOW(), NOW()),
('CRS007', 'Signals and Systems', 2, 7, 'Electronics', NOW(), NOW()),
('CRS008', 'Manufacturing Processes', 3, 8, 'Mechanical', NOW(), NOW()),
('CRS009', 'Transportation Engineering', 4, 1, 'Civil', NOW(), NOW()),
('CRS010', 'Cloud Computing', 2, 2, 'Information Technology', NOW(), NOW());

WITH RECURSIVE student_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM student_seq WHERE n < 50
)
INSERT INTO students (student_id, first_name, last_name, email, phone, dob, gender, address, department, semester, joining_date, status, image_url, created_at, updated_at)
SELECT
    CONCAT('STU', LPAD(n, 3, '0')),
    ELT(((n - 1) MOD 10) + 1, 'Arjun', 'Saanvi', 'Vivaan', 'Anaya', 'Aditya', 'Diya', 'Krishna', 'Anika', 'Vihaan', 'Sara'),
    ELT(((n - 1) MOD 10) + 1, 'Patel', 'Rao', 'Gupta', 'Singh', 'Kumar', 'Menon', 'Yadav', 'Shah', 'Mishra', 'Pillai'),
    CONCAT('student', n, '@campus.local'),
    CONCAT('800000', LPAD(n, 4, '0')),
    DATE_ADD('2001-01-01', INTERVAL (n * 17) DAY),
    ELT(((n - 1) MOD 3) + 1, 'FEMALE', 'MALE', 'OTHER'),
    CONCAT(10 + n, ', University Avenue, Academic City'),
    ELT(((n - 1) MOD 5) + 1, 'Computer Science', 'Electronics', 'Mechanical', 'Civil', 'Information Technology'),
    ((n - 1) MOD 8) + 1,
    DATE_SUB(CURDATE(), INTERVAL ((n MOD 24) + 1) MONTH),
    CASE WHEN MOD(n, 9) = 0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
    CONCAT('https://example.com/student-', n, '.png'),
    NOW(),
    NOW()
FROM student_seq;

WITH RECURSIVE course_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM course_seq WHERE n < 10
)
INSERT INTO faculty_courses (faculty_id, course_id, assigned_date)
SELECT
    f.id,
    c.id,
    DATE_SUB(CURDATE(), INTERVAL cs.n DAY)
FROM course_seq cs
JOIN courses c ON c.course_code = CONCAT('CRS', LPAD(cs.n, 3, '0'))
JOIN faculty f ON f.faculty_id IN (
    CONCAT('FAC', LPAD(cs.n, 3, '0')),
    CONCAT('FAC', LPAD(((cs.n) MOD 10) + 1, 3, '0'))
);

WITH RECURSIVE enroll_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM enroll_seq WHERE n < 150
)
INSERT INTO student_courses (course_id, student_id)
SELECT
    c.id,
    s.id
FROM enroll_seq es
JOIN courses c ON c.course_code = CONCAT('CRS', LPAD(((es.n - 1) MOD 10) + 1, 3, '0'))
JOIN students s ON s.student_id = CONCAT('STU', LPAD(((es.n * 3 - 1) MOD 50) + 1, 3, '0'))
ON DUPLICATE KEY UPDATE course_id = VALUES(course_id);

WITH RECURSIVE attendance_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM attendance_seq WHERE n < 500
)
INSERT INTO attendance (student_id, faculty_id, course_id, attendance_date, status, remarks, created_at, updated_at)
SELECT
    s.id,
    f.id,
    c.id,
    DATE_SUB(CURDATE(), INTERVAL ((a.n - 1) MOD 40) DAY),
    CASE WHEN MOD(a.n, 5) = 0 THEN 'ABSENT' ELSE 'PRESENT' END,
    CASE WHEN MOD(a.n, 5) = 0 THEN 'Medical leave approved' ELSE 'Present in class' END,
    NOW(),
    NOW()
FROM attendance_seq a
JOIN courses c ON c.course_code = CONCAT('CRS', LPAD(((a.n - 1) MOD 10) + 1, 3, '0'))
JOIN students s ON s.student_id = CONCAT('STU', LPAD(((a.n * 7 - 1) MOD 50) + 1, 3, '0'))
JOIN faculty f ON f.faculty_id = CONCAT('FAC', LPAD(((a.n - 1) MOD 10) + 1, 3, '0'))
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    remarks = VALUES(remarks),
    updated_at = NOW();

WITH RECURSIVE audit_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM audit_seq WHERE n < 100
)
INSERT INTO audit_logs (username, role, action, entity_name, entity_id, description, timestamp, ip_address)
SELECT
    CASE WHEN MOD(n, 3) = 0 THEN 'admin' ELSE 'faculty' END,
    CASE WHEN MOD(n, 3) = 0 THEN 'ADMIN' ELSE 'FACULTY' END,
    ELT(((n - 1) MOD 5) + 1, 'LOGIN', 'CREATE', 'UPDATE', 'DELETE', 'LOGOUT'),
    ELT(((n - 1) MOD 6) + 1, 'AUTH', 'STUDENT', 'FACULTY', 'COURSE', 'ATTENDANCE', 'USER'),
    CAST(n AS CHAR(20)),
    CONCAT('Seeded audit log entry ', n),
    DATE_SUB(NOW(), INTERVAL n HOUR),
    CONCAT('127.0.0.', ((n - 1) MOD 20) + 1)
FROM audit_seq;
