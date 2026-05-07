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
        String starQuery = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, NULL)";
        String starMovieQuery = "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPass);
             PreparedStatement starStatement = conn.prepareStatement(starQuery);
             PreparedStatement starMovieStatement = conn.prepareStatement(starMovieQuery)) {

            conn.setAutoCommit(false);

            int batchCount = 0;

            while (true) {
                CastInsertTask task = queue.take();

                if (task.isPoisonPill()) {
                    break;
                }

                if (task.isNewStar()) {
                    starStatement.setString(1, task.getStarId());
                    starStatement.setString(2, task.getStarName());
                    starStatement.addBatch();
                }

                starMovieStatement.setString(1, task.getStarId());
                starMovieStatement.setString(2, task.getMovieId());
                starMovieStatement.addBatch();

                batchCount++;

                if (batchCount >= BATCH_SIZE) {
                    starStatement.executeBatch();
                    starMovieStatement.executeBatch();
                    conn.commit();
                    batchCount = 0;
                }
            }

            starStatement.executeBatch();
            starMovieStatement.executeBatch();
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
