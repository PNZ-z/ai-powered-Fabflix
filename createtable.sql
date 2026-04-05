DROP DATABASE IF EXISTS moviedb;
CREATE DATABASE moviedb;
USE moviedb;
CREATE TABLE movies(
    id varchar(10) PRIMARY KEY,
    title varchar(100) NOT NULL ,
    year int NOT NULL ,
    director varchar(100) NOT NULL
);
CREATE TABLE stars(
    id varchar(10) PRIMARY KEY,
    name varchar(100) NOT NULL,
    birth_year int
);
CREATE TABLE  stars_in_movies(
     star_id varchar(10) NOT NULL ,
     movie_id varchar(10) NOT NULL,
     FOREIGN KEY (star_id) references stars(id),
     FOREIGN KEY (movie_id) references movies(id)
);
CREATE TABLE genres(
    id int AUTO_INCREMENT PRIMARY KEY,
    name varchar(32) NOT NULL
);
CREATE TABLE genres_in_movies(
    genre_id int NOT NULL,
    movie_id varchar(10) NOT NULL,
    FOREIGN KEY (genre_id) references genres(id),
    FOREIGN KEY (movie_id) references movies(id)
);
CREATE TABLE credit_cards (
    id varchar(20) PRIMARY KEY ,
    first_name varchar(50) NOT NULL ,
    last_name varchar(50) NOT NULL ,
    expiration date NOT NULL
);
CREATE TABLE customers(
    id int AUTO_INCREMENT PRIMARY KEY ,
    first_name varchar(50) NOT NULL,
    last_name varchar(50) NOT NULL,
    credit_card_id varchar(20) NOT NULL,
    address varchar(200) NOT NULL,
    email varchar(50) NOT NULL,
    password varchar(20) NOT NULL,
    FOREIGN KEY (credit_card_id) references credit_cards(id)
);

CREATE TABLE sales(
    id int AUTO_INCREMENT PRIMARY KEY,
    customer_id int NOT NULL,
    movie_id varchar(10) NOT NULL,
    sale_date date NOT NULL,
    FOREIGN KEY (customer_id) references customers(id),
    foreign key (movie_id) references movies(id)
);

CREATE TABLE ratings(
    movie_id varchar(10) NOT NULL,
    rating float NOT NULL,
    vote_count integer NOT NULL,
    foreign key (movie_id) references movies(id)
);