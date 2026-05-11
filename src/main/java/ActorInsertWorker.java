import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.concurrent.BlockingQueue;

public class ActorInsertWorker implements Runnable {
    private static final int BATCH_SIZE = 1000;

    private final BlockingQueue<ActorInsertTask> queue;
    private final String loginUrl;
    private final String loginUser;
    private final String loginPass;

    public ActorInsertWorker(
            BlockingQueue<ActorInsertTask> queue,
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
        String insertStarQuery = "INSERT INTO stars (id, name, birth_year) VALUES (?, ?, ?)";
        String updateBirthYearQuery = "UPDATE stars SET birth_year = ? WHERE id = ? AND birth_year IS NULL";

        try (Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPass);
             PreparedStatement insertStarStatement = conn.prepareStatement(insertStarQuery);
             PreparedStatement updateBirthYearStatement = conn.prepareStatement(updateBirthYearQuery)) {

            conn.setAutoCommit(false);

            int insertBatchCount = 0;
            int updateBatchCount = 0;

            while (true) {
                ActorInsertTask task = queue.take();

                if (task.isPoisonPill()) {
                    break;
                }

                if (task.getType() == ActorInsertTask.TaskType.INSERT_STAR) {
                    insertStarStatement.setString(1, task.getStarId());
                    insertStarStatement.setString(2, task.getStarName());

                    if (task.getBirthYear() == null) {
                        insertStarStatement.setNull(3, Types.INTEGER);
                    } else {
                        insertStarStatement.setInt(3, task.getBirthYear());
                    }

                    insertStarStatement.addBatch();
                    insertBatchCount++;
                } else if (task.getType() == ActorInsertTask.TaskType.UPDATE_BIRTH_YEAR) {
                    updateBirthYearStatement.setInt(1, task.getBirthYear());
                    updateBirthYearStatement.setString(2, task.getStarId());
                    updateBirthYearStatement.addBatch();
                    updateBatchCount++;
                }

                if (insertBatchCount >= BATCH_SIZE) {
                    insertStarStatement.executeBatch();
                    conn.commit();
                    insertBatchCount = 0;
                }

                if (updateBatchCount >= BATCH_SIZE) {
                    updateBirthYearStatement.executeBatch();
                    conn.commit();
                    updateBatchCount = 0;
                }
            }

            if (insertBatchCount > 0) {
                insertStarStatement.executeBatch();
            }

            if (updateBatchCount > 0) {
                updateBirthYearStatement.executeBatch();
            }

            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
