-- Course Registration System: database of the accounts and the course catalog
-- Run in SQL Server Management Studio, connected to the local SQL Server, with GlassFish stopped.
-- It rebuilds the whole database with its demo data, so it also serves as the reset before a demonstration.

-- new: drops the database if it already exists, closing any open connection first, so the script can run again
USE master;
GO

IF DB_ID('CourseRegistrationdb') IS NOT NULL
BEGIN
    ALTER DATABASE CourseRegistrationdb SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE CourseRegistrationdb;
END
GO

CREATE DATABASE CourseRegistrationdb;
GO

USE CourseRegistrationdb;
GO

-- the four access levels; Users.role must be one of them (foreign key)
CREATE TABLE Roles (
    role        NVARCHAR(10)  NOT NULL PRIMARY KEY,
    accesslevel INT           NOT NULL UNIQUE,
    description NVARCHAR(100) NOT NULL
);
GO

INSERT INTO Roles (role, accesslevel, description) VALUES
('STUDENT', 0, 'Registers in sections and pays tuition'),
('CLERK',   1, 'Registers students and records payments'),
('MANAGER', 2, 'Manages courses and sections, reads reports'),
('ADMIN',   3, 'Manages staff accounts');
GO

-- studentid is 0 for staff accounts (ADMIN, MANAGER, CLERK)
CREATE TABLE Users (
    userid       INT           NOT NULL PRIMARY KEY,
    username     NVARCHAR(100) NOT NULL UNIQUE,
    passwordhash CHAR(64)      NOT NULL,
    role         NVARCHAR(10)  NOT NULL REFERENCES Roles(role),
    studentid    INT           NOT NULL
);
GO

-- the 18 accounts of the seed copy of User.in
INSERT INTO Users (userid, username, passwordhash, role, studentid) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN', 0),
(2, 'manager', '866485796cfa8d7c0cf7111640205b83076433547577511d81f8030ae99ecea5', 'MANAGER', 0),
(3, 'clerk', 'a3630b8b8f6c82d33b0695f77f915e69ed7b0c5214062f8b870219845e069d30', 'CLERK', 0),
(4, 'liam.tremblay@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 1),
(5, 'priya.sharma@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 2),
(6, 'mei.chen@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 3),
(7, 'fatima.alrashid@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 4),
(8, 'noah.gagnon@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 5),
(9, 'aisha.okafor@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 6),
(10, 'santiago.morales@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 7),
(11, 'yuki.tanaka@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 8),
(12, 'rania.belkacem@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 9),
(13, 'marcus.osei@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 10),
(14, 'leila.nasser@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 11),
(15, 'lucas.pelletier@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 12),
(16, 'amara.kone@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 13),
(17, 'soren.christensen@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 14),
(18, 'valentina.reyes@gmail.com', '703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b', 'STUDENT', 15);
GO

-- new: the departments, moved from Department.in
CREATE TABLE Department (
    departmentid   INT           NOT NULL PRIMARY KEY,
    departmentname NVARCHAR(100) NOT NULL,
    departmenthead NVARCHAR(100) NOT NULL
);
GO

INSERT INTO Department (departmentid, departmentname, departmenthead) VALUES
(1, 'Computer Science', 'Margaret Hamilton'),
(2, 'Mathematics', 'John von Neumann'),
(3, 'Software Engineering', 'Barbara Liskov'),
(4, 'Natural Sciences', 'Richard Feynman'),
(5, 'Humanities and Philosophy', 'Hannah Arendt');
GO

-- new: the courses, moved from Course.in; the code is unique and the department must exist (foreign key)
CREATE TABLE Course (
    courseid     INT           NOT NULL PRIMARY KEY,
    coursecode   NVARCHAR(10)  NOT NULL UNIQUE,
    coursename   NVARCHAR(100) NOT NULL,
    credits      INT           NOT NULL,
    departmentid INT           NOT NULL REFERENCES Department(departmentid)
);
GO

INSERT INTO Course (courseid, coursecode, coursename, credits, departmentid) VALUES
(1, 'CS101', 'Programming Concepts 1', 3, 1),
(2, 'CS201', 'Database and Algorithms', 3, 1),
(3, 'CS301', 'Operating Systems', 3, 1),
(4, 'CS401', 'Artificial Intelligence', 3, 1),
(5, 'MA101', 'Calculus I', 3, 2),
(6, 'MA201', 'Calculus II', 3, 2),
(7, 'MA301', 'Linear Algebra', 3, 2),
(8, 'SE101', 'Software Design Principles', 3, 3),
(9, 'SE201', 'Database Design', 3, 3),
(10, 'SE301', 'Web Application Development', 3, 3),
(11, 'NS101', 'Introduction to Physics', 3, 4),
(12, 'NS201', 'Introduction to Chemistry', 3, 4),
(13, 'HU101', 'Logic and Critical Thinking', 3, 5),
(14, 'HU201', 'Ethics in Technology', 3, 5);
GO

-- expected: 18 accounts, 5 departments, 14 courses
SELECT (SELECT COUNT(*) FROM Users) AS accounts,
       (SELECT COUNT(*) FROM Department) AS departments,
       (SELECT COUNT(*) FROM Course) AS courses;
