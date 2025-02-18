-- Drop database if exists and recreate it
DROP DATABASE IF EXISTS office_management;
CREATE DATABASE office_management;
\c office_management;

-- Drop tables if they exist (in correct order due to foreign keys)
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS office_rooms;
DROP TABLE IF EXISTS floors;

-- Drop sequences if they exist
DROP SEQUENCE IF EXISTS employee_seq;
DROP SEQUENCE IF EXISTS seat_seq;
DROP SEQUENCE IF EXISTS office_room_seq;
DROP SEQUENCE IF EXISTS floor_seq;

-- Create sequences
CREATE SEQUENCE employee_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seat_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE office_room_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE floor_seq START WITH 1 INCREMENT BY 1;

-- Create tables in correct order (no forward references)
CREATE TABLE floors (
    id BIGINT DEFAULT nextval('floor_seq') PRIMARY KEY,
    floor_number INTEGER NOT NULL,
    name VARCHAR(255)
);

CREATE TABLE office_rooms (
    id BIGINT DEFAULT nextval('office_room_seq') PRIMARY KEY,
    room_number VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    floor_id BIGINT REFERENCES floors(id)
);

CREATE TABLE employees (
    id BIGINT DEFAULT nextval('employee_seq') PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    occupation VARCHAR(255) NOT NULL
);

CREATE TABLE seats (
    id BIGINT DEFAULT nextval('seat_seq') PRIMARY KEY,
    seat_number VARCHAR(255) NOT NULL,
    room_id BIGINT REFERENCES office_rooms(id),
    employee_id BIGINT REFERENCES employees(id) -- add UNIQUE if the employee can only have one seat
);

-- Insert sample data in correct order
-- 1. First, insert floors
INSERT INTO floors (floor_number, name) VALUES
(1, 'First Floor'),
(2, 'Second Floor'),
(3, 'Third Floor'),
(4, 'Fourth Floor'),
(5, 'Fifth Floor'),
(6, 'Sixth Floor'),
(7, 'Seventh Floor'),
(8, 'Eighth Floor'),
(9, 'Ninth Floor');

-- 2. Then, insert rooms for each floor
DO $$
DECLARE
    floor_record RECORD;
BEGIN
    FOR floor_record IN SELECT id, floor_number FROM floors ORDER BY floor_number
    LOOP
        FOR room_num IN 1..20
        LOOP
            INSERT INTO office_rooms (room_number, name, floor_id)
            VALUES (
                CONCAT(floor_record.floor_number, LPAD(room_num::text, 2, '0')),
                CONCAT('Room ', floor_record.floor_number, LPAD(room_num::text, 2, '0')),
                floor_record.id
            );
        END LOOP;
    END LOOP;
END $$;

-- 3. Insert seats for each room
DO $$
DECLARE
    room_record RECORD;
BEGIN
    FOR room_record IN SELECT id, room_number FROM office_rooms
    LOOP
        FOR seat_num IN 1..4
        LOOP
            INSERT INTO seats (seat_number, room_id)
            VALUES (
                CONCAT(room_record.room_number, '-', LPAD(seat_num::text, 2, '0')),
                room_record.id
            );
        END LOOP;
    END LOOP;
END $$; 


-- 4. Finally some employee data
INSERT INTO employees (full_name, occupation) VALUES
-- Italian Names
('Marco Rossi', 'Senior Software Architect'),
('Giuseppe Conti', 'DevOps Engineer'),
('Alessandro Ferrari', 'Security Systems Specialist'),
('Sofia Marino', 'Data Privacy Officer'),
('Lorenzo Romano', 'Full Stack Developer'),
('Valentina Colombo', 'Systems Analyst'),
('Luca Ricci', 'Database Administrator'),
('Matteo Greco', 'Cloud Infrastructure Engineer'),
('Chiara Esposito', 'Scrum Master'),
('Andrea Moretti', 'Software Development Team Lead'),
('Francesca Barbieri', 'Quality Assurance Engineer'),
('Roberto Mancini', 'Network Security Engineer'),
('Elena Lombardi', 'Business Analyst'),
('Paolo Gallo', 'Backend Developer'),
('Isabella Costa', 'Frontend Developer'),
('Davide Fontana', 'Infrastructure Architect'),
('Giulia Santoro', 'UX/UI Designer'),
('Antonio Marini', 'System Integration Specialist'),
('Claudia Vitale', 'Information Security Analyst'),
('Stefano Leone', 'API Integration Specialist'),
('Maria Longo', 'Technical Project Manager'),
('Fabio Ferrara', 'DevSecOps Engineer'),
('Laura Pellegrini', 'Data Engineer'),
('Vincenzo Serra', 'Software Engineer'),
('Cristina Palumbo', 'Agile Coach'),
('Emilio Valentini', 'Solutions Architect'),
('Silvia Monti', 'Product Owner'),
('Dario Battaglia', 'Site Reliability Engineer'),
('Beatrice Farina', 'Quality Assurance Lead'),
('Massimo Rizzi', 'Enterprise Architect'),
('Valeria Caruso', 'Technical Writer'),
('Nicola De Luca', 'Release Manager'),
('Elisa Martini', 'Software Test Engineer'),
('Simone Gatti', 'Cloud Security Engineer'),
('Alessia Bernardi', 'IT Compliance Specialist'),

-- German Names
('Hans Mueller', 'Principal Software Engineer'),
('Wolfgang Schmidt', 'Security Operations Lead'),
('Klaus Weber', 'Technical Architect'),
('Gerhard Fischer', 'DevOps Team Lead'),
('Dieter Wagner', 'Systems Security Engineer'),
('Markus Becker', 'Cloud Platform Engineer'),
('Stefan Hoffmann', 'Software Development Manager'),
('Thomas Schulz', 'Integration Specialist'),
('Michael Koch', 'Database Security Specialist'),
('Andreas Bauer', 'Infrastructure Security Engineer'),
('Jürgen Richter', 'Senior Systems Engineer'),
('Werner Klein', 'Application Security Engineer'),
('Rainer Wolf', 'Network Engineer'),
('Erich Schröder', 'IT Security Architect'),
('Karl Neumann', 'Software Quality Engineer'),
('Sabine Meyer', 'Data Protection Specialist'),
('Monika Krause', 'Agile Project Manager'),
('Ursula Schwarz', 'Information Systems Security Officer'),
('Helga Zimmermann', 'Requirements Engineer'),
('Ingrid Schmitt', 'Configuration Manager'),
('Petra Lange', 'IT Governance Specialist'),
('Renate Krüger', 'Quality Management Lead'),
('Brigitte Hartmann', 'Documentation Specialist'),
('Erika Werner', 'Process Automation Engineer'),
('Heinrich Schmitz', 'Security Compliance Officer'),
('Otto Meier', 'Infrastructure Manager'),
('Fritz Lehmann', 'Systems Integration Engineer'),
('Walter König', 'Technical Operations Specialist'),
('Gustav Huber', 'Enterprise Solutions Architect'),
('Wilhelm Braun', 'Cloud Operations Engineer'),
('Manfred Berg', 'IT Risk Analyst'),
('Rudolf Fuchs', 'Cybersecurity Engineer'),
('Ernst Keller', 'Platform Engineer'),
('Hermann Vogel', 'Security Systems Architect'),
('Kurt Frank', 'Technical Support Lead'),
('Günther Berger', 'Systems Administrator'),
('Ludwig Kaiser', 'Network Operations Engineer'),
('Helmut Schuster', 'IT Auditor'); 