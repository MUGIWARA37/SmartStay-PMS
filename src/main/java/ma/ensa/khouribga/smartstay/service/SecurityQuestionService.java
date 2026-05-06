package ma.ensa.khouribga.smartstay.service;

import ma.ensa.khouribga.smartstay.dao.SecurityQuestionDao;
import java.sql.SQLException;
import java.util.List;

public class SecurityQuestionService {

    public List<SecurityQuestionDao.Question> getAllActiveQuestions() throws SQLException {
        return SecurityQuestionDao.findAll();
    }

    public List<SecurityQuestionDao.Question> getQuestionsForUser(long userId) throws SQLException {
        return SecurityQuestionDao.findByUser(userId);
    }

    public boolean userHasSecurityAnswers(long userId) throws SQLException {
        return SecurityQuestionDao.hasAnswers(userId);
    }

    public void saveUserSecurityAnswers(long userId, long q1, String a1, long q2, String a2, long q3, String a3) throws SQLException {
        SecurityQuestionDao.saveAnswers(userId, q1, a1, q2, a2, q3, a3);
    }

    public boolean verifyUserSecurityAnswers(long userId, long q1, String a1, long q2, String a2, long q3, String a3) throws SQLException {
        return SecurityQuestionDao.verifyAllAnswers(userId, q1, a1, q2, a2, q3, a3);
    }

    public void resetUserPassword(long userId, String newPlainPassword) throws SQLException {
        SecurityQuestionDao.resetPassword(userId, newPlainPassword);
    }

    public long getUserIdByUsername(String username) throws SQLException {
        return SecurityQuestionDao.findUserIdByUsername(username);
    }
}
