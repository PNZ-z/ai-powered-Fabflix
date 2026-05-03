USE moviedb;

DROP PROCEDURE IF EXISTS add_movie;

DELIMITER $$

CREATE PROCEDURE add_movie(
    IN p_movie_title VARCHAR(100),
    IN p_movie_year INT,
    IN p_movie_director VARCHAR(100),
    IN p_star_name VARCHAR(100),
    IN p_star_birth_year INT,
    IN p_genre_name VARCHAR(32)
)
BEGIN
    DECLARE v_movie_id VARCHAR(10);
    DECLARE v_star_id VARCHAR(10);
    DECLARE v_genre_id INT;

    DECLARE v_max_movie_id VARCHAR(10);
    DECLARE v_max_star_id VARCHAR(10);
    DECLARE v_max_genre_id INT;

    DECLARE v_movie_count INT DEFAULT 0;
    DECLARE v_status_message VARCHAR(1000) DEFAULT '';

    SELECT COUNT(*)
    INTO v_movie_count
    FROM movies
    WHERE title = p_movie_title
      AND year = p_movie_year
      AND director = p_movie_director;

    IF v_movie_count > 0 THEN

        SELECT
            'fail' AS status,
            CONCAT(
                    'Movie already exists: ',
                    p_movie_title,
                    ' (',
                    p_movie_year,
                    '), directed by ',
                    p_movie_director,
                    '. No changes were made.'
            ) AS message;

    ELSE

        SELECT MAX(id)
        INTO v_max_movie_id
        FROM movies;

        SET v_movie_id = CONCAT(
                'tt',
                LPAD(CAST(CAST(SUBSTRING(v_max_movie_id, 3) AS UNSIGNED) + 1 AS CHAR), 7, '0')
                         );

        INSERT INTO movies (id, title, year, director)
        VALUES (v_movie_id, p_movie_title, p_movie_year, p_movie_director);

        SET v_status_message = CONCAT(
                'Movie added successfully with ID ',
                v_movie_id,
                '. '
                               );

        SELECT id
        INTO v_star_id
        FROM stars
        WHERE name = p_star_name
        LIMIT 1;

        IF v_star_id IS NULL THEN

            SELECT MAX(id)
            INTO v_max_star_id
            FROM stars;

            SET v_star_id = CONCAT(
                    'nm',
                    LPAD(CAST(CAST(SUBSTRING(v_max_star_id, 3) AS UNSIGNED) + 1 AS CHAR), 7, '0')
                            );

            INSERT INTO stars (id, name, birth_year)
            VALUES (v_star_id, p_star_name, p_star_birth_year);

            SET v_status_message = CONCAT(
                    v_status_message,
                    'New star created with ID ',
                    v_star_id,
                    '. '
                                   );

        ELSE

            SET v_status_message = CONCAT(
                    v_status_message,
                    'Existing star linked with ID ',
                    v_star_id,
                    '. '
                                   );

        END IF;

        INSERT INTO stars_in_movies (star_id, movie_id)
        VALUES (v_star_id, v_movie_id);

        SELECT id
        INTO v_genre_id
        FROM genres
        WHERE name = p_genre_name
        LIMIT 1;

        IF v_genre_id IS NULL THEN

            SELECT MAX(id)
            INTO v_max_genre_id
            FROM genres;

            SET v_genre_id = v_max_genre_id + 1;

            INSERT INTO genres (id, name)
            VALUES (v_genre_id, p_genre_name);

            SET v_status_message = CONCAT(
                    v_status_message,
                    'New genre created with ID ',
                    v_genre_id,
                    '. '
                                   );

        ELSE

            SET v_status_message = CONCAT(
                    v_status_message,
                    'Existing genre linked with ID ',
                    v_genre_id,
                    '. '
                                   );

        END IF;

        INSERT INTO genres_in_movies (genre_id, movie_id)
        VALUES (v_genre_id, v_movie_id);

        SELECT
            'success' AS status,
            v_status_message AS message;

    END IF;

END $$

DELIMITER ;