package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SecurityQuestionDao {

    // ── Model ─────────────────────────────────────────────────────────────────
    public record Question(long id, String text) {}

    // ── Read ──────────────────────────────────────────────────────────────────

    /** All active questions from the catalogue. */
    public static List<Question> findAll() throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT id, question_text FROM security_questions WHERE is_active = 1 ORDER BY id";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(new Question(rs.getLong("id"), rs.getString("question_text")));
        }
        return list;
    }

    /** Questions chosen by a specific user (for the recovery flow). */
    public static List<Question> findByUser(long userId) throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = """
            SELECT sq.id, sq.question_text
            FROM user_security_answers usa
            JOIN security_questions sq ON sq.id = usa.question_id
            WHERE usa.user_id = ?
            ORDER BY usa.id
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new Question(rs.getLong("id"), rs.getString("question_text")));
            }
        }
        return list;
    }

    /** Returns true if the user has already saved security answers. */
    public static boolean hasAnswers(long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_security_answers WHERE user_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Saves 3 question-answer pairs for a user.
     * Answers are BCrypt-hashed before storage.
     * Any existing answers are replaced atomically.
     */
    public static void saveAnswers(long userId,
                                   long q1, String a1,
                                   long q2, String a2,
                                   long q3, String a3) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Delete old answers first
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM user_security_answers WHERE user_id = ?")) {
                    ps.setLong(1, userId); ps.executeUpdate();
                }
                String ins = "INSERT INTO user_security_answers (user_id, question_id, answer_hash) VALUES (?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(ins)) {
                    for (long[] qa : new long[][]{{q1}, {q2}, {q3}}) { /* handled below */ }
                    // Insert row 1
                    ps.setLong(1, userId); ps.setLong(2, q1);
                    ps.setString(3, BCrypt.hashpw(a1.trim().toLowerCase(), BCrypt.gensalt(10)));
                    ps.executeUpdate();
                    // Insert row 2
                    ps.setLong(2, q2);
                    ps.setString(3, BCrypt.hashpw(a2.trim().toLowerCase(), BCrypt.gensalt(10)));
                    ps.executeUpdate();
                    // Insert row 3
                    ps.setLong(2, q3);
                    ps.setString(3, BCrypt.hashpw(a3.trim().toLowerCase(), BCrypt.gensalt(10)));
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback(); throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    /**
     * Verifies all 3 answers for a user during recovery.
     * Returns true only if every answer matches its stored BCrypt hash.
     */
    public static boolean verifyAllAnswers(long userId,
                                           long q1, String a1,
                                           long q2, String a2,
                                           long q3, String a3) throws SQLException {
        String sql = "SELECT question_id, answer_hash FROM user_security_answers WHERE user_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                int matched = 0;
                while (rs.next()) {
                    long qId   = rs.getLong("question_id");
                    String hash = rs.getString("answer_hash");
                    String provided =
                        qId == q1 ? a1.trim().toLowerCase() :
                        qId == q2 ? a2.trim().toLowerCase() :
                        qId == q3 ? a3.trim().toLowerCase() : null;
                    if (provided != null && BCrypt.checkpw(provided, hash)) matched++;
                }
                return matched == 3;
            }
        }
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    /** Sets a new BCrypt-hashed password for the given user. */
    public static void resetPassword(long userId, String newPlainPassword) throws SQLException {
        String hash = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(12));
        String sql  = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    /** Looks up a user id by username (for recovery — no active check needed). */
    public static long findUserIdByUsername(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ? LIMIT 1";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : -1L;
            }
        }
    }
}
