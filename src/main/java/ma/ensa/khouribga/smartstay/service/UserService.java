package ma.ensa.khouribga.smartstay.service;

import ma.ensa.khouribga.smartstay.dao.UserDao;
import ma.ensa.khouribga.smartstay.model.User;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserService {

    public List<User> getAllUsers() throws SQLException {
        return UserDao.findAll();
    }

    public Optional<User> getUserById(long id) throws SQLException {
        return UserDao.findById(id);
    }

    public Optional<User> getUserByUsername(String username) throws SQLException {
        return UserDao.findByUsername(username);
    }

    public List<User> getUsersByRole(User.Role role) throws SQLException {
        return UserDao.findByRole(role);
    }

    public void updateProfilePicture(long userId, String picturePath) throws SQLException {
        UserDao.updateProfilePicture(userId, picturePath);
    }

    public void updateLastLogin(long userId) throws SQLException {
        UserDao.updateLastLogin(userId);
    }

    public boolean setUserActive(long userId, boolean active) throws SQLException {
        return UserDao.setActive(userId, active);
    }

    public long createUser(String username, String email, String password, User.Role role) throws SQLException {
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        return UserDao.insert(username, email, passwordHash, role);
    }

    public Optional<User> authenticate(String username, String password) throws SQLException {
        Optional<User> userOpt = UserDao.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (BCrypt.checkpw(password, user.getPasswordHash())) {
                updateLastLogin(user.getId());
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public String getStaffPosition(long userId) throws SQLException {
        String sql = "SELECT position FROM staff_profiles WHERE user_id = ? LIMIT 1";
        try (java.sql.Connection c = ma.ensa.khouribga.smartstay.db.Database.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("position").toLowerCase() : "";
            }
        }
    }
}
