import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.concurrent.BlockingQueue;

public class CastInsertWorker implements Runnable {
    private static final int BATCH_SIZE = 1000;

    private final BlockingQueue<CastInsertTask> queue;
    private final String loginUrl;
    private final String loginUser;
    private final String loginPass;

    public CastInsertWorker(
            BlockingQueue<CastInsertTask> queue,
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
        String starMovieQuery = "INSERT INTO stars_in_movies (star_id, movie_id) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPass);
             PreparedStatement starMovieStatement = conn.prepareStatement(starMovieQuery)) {

            conn.setAutoCommit(false);

            int batchCount = 0;

            while (true) {
                CastInsertTask task = queue.take();

                if (task.isPoisonPill()) {
                    break;
                }

                starMovieStatement.setString(1, task.getStarId());
                starMovieStatement.setString(2, task.getMovieId());
                starMovieStatement.addBatch();

                batchCount++;

                if (batchCount >= BATCH_SIZE) {
                    starMovieStatement.executeBatch();
                    conn.commit();
                    batchCount = 0;
                }
            }

            starMovieStatement.executeBatch();
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
