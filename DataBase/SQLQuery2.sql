USE StreamNetDB;

CREATE TABLE Users (
    id INT IDENTITY PRIMARY KEY,
    username NVARCHAR(50) NOT NULL,
    password NVARCHAR(100) NOT NULL,
    role NVARCHAR(10) CHECK (role IN ('user', 'streamer')) NOT NULL
);