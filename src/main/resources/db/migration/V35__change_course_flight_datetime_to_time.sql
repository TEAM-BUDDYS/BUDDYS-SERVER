ALTER TABLE course_flight
    CHANGE COLUMN departure_at departure_time TIME NOT NULL,
    CHANGE COLUMN arrival_at arrival_time TIME NOT NULL;
