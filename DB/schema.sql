-- create table funds --
CREATE TABLE if not exists funds(id varchar(50) PRIMARY KEY,name varchar(70) not null unique,amount double,date DATE not null,description text,balance double);


-- create table transactions 
CREATE TABLE if not exists transactions(id varchar(50) PRIMARY KEY, amount double not null, who varchar(30), date date, time time, description varchar(300), type varchar(20), fundId varchar(50),balance double,foreign key(fundId) references funds(id) ON DELETE CASCADE ON UPDATE CASCADE);
