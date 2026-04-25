package ma.ensa.khouribga.smartstay.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Simple transaction helper.
 *
 * Usage:
 *   TxManager.runInTransaction(conn -> {
 *       // all DB work here — auto-committed on success, rolled back on exception
 *       conn.prepareStatement("INSERT ...").executeUpdate();
 *       conn.prepareStatement("UPDATE ...").executeUpdate();
 *   });
 */
public final class TxManager {

    private TxManager() { /* utility class */ }

    /**
     * Functional interface for code that runs inside a transaction.
     * Allows checked SQLExceptions to propagate naturally.
     */
    @FunctionalInterface
    public interface TransactionalWork {
        void execute(Connection conn) throws SQLException;
    }

    /**
     * Executes the given work inside a single JDBC transaction.
     * Commits on success, rolls back on any exception, and always
     * returns the connection to the pool.
     *
     * @param work the database operations to perform
     * @throws RuntimeException wrapping the original SQLException if anything fails
     */
    public static void runInTransaction(TransactionalWork work) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            work.execute(conn);

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // restore default before returning to pool
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    /**
     * Convenience overload: runs work and returns a result.
     *
     * Usage:
     *   int newId = TxManager.runInTransaction(conn -> {
     *       ...
     *       return generatedKey;
     *   });
     */
    @FunctionalInterface
    public interface TransactionalSupplier<T> {
        T execute(Connection conn) throws SQLException;
    }

    public static <T> T runInTransaction(TransactionalSupplier<T> work) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            T result = work.execute(conn);

            conn.commit();
            return result;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }
}