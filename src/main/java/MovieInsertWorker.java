import java.sql.*;
import java.util.concurrent.BlockingQueue;

public class MovieInsertWorker implements Runnable {
    private static final int BATCH_SIZE = 1000;

    private final BlockingQueue<MovieInsertTask> queue;
    private final String loginUrl;
    private final String loginUser;
    private final String loginPass;

    public MovieInsertWorker(
            BlockingQueue<MovieInsertTask> queue,
            String loginUrl,
            String loginUser,
            String loginPass
    ) {
        this.queue = queue;
        this.loginUrl = loginUrl;
        this.loginUser = loginUser;
        this.loginPass = loginPass;
    }

    @Override
    public void run() {
        String movieQuery = "INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)";
        String genreQuery = "INSERT INTO genres_in_movies (genre_id, movie_id) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPass);
             PreparedStatement movieStmt = conn.prepareStatement(movieQuery);
             PreparedStatement genreStmt = conn.prepareStatement(genreQuery)) {

            conn.setAutoCommit(false);
            int batchCount = 0;

            while (true) {
                MovieInsertTask task = queue.take();

                if (task.isPoisonPill()) {
                    break;
                }

                Movie mov = task.getMovie();

                movieStmt.setString(1, mov.getMovieID());
                movieStmt.setString(2, mov.getTitle());
                movieStmt.setInt(3, mov.getYear());
                movieStmt.setString(4, mov.getDirector());
                movieStmt.addBatch();

                for (Integer genreId : task.getGenreIds()) {
                    genreStmt.setInt(1, genreId);
                    genreStmt.setString(2, mov.getMovieID());
                    genreStmt.addBatch();
                }

                batchCount++;

                if (batchCount >= BATCH_SIZE) {
                    movieStmt.executeBatch();
                    genreStmt.executeBatch();
                    conn.commit();
                    batchCount = 0;
                }
            }

            movieStmt.executeBatch();
            genreStmt.executeBatch();
            conn.commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
