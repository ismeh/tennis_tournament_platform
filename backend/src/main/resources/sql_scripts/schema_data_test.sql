create table COUNTRIES(
  ID int not null AUTO_INCREMENT,
  NAME varchar(100) not null,
  STATUS int,
  PRIMARY KEY ( ID )
);

INSERT INTO countries (id, name) VALUES (1, 'USA');
INSERT INTO countries (id, name) VALUES (2, 'France');
INSERT INTO countries (id, name) VALUES (3, 'Brazil');
INSERT INTO countries (id, name) VALUES (4, 'Italy');
INSERT INTO countries (id, name) VALUES (5, 'Canada');