package ma.ensa.khouribga.smartstay.session;

import ma.ensa.khouribga.smartstay.model.User;

public final class SessionManager {
    private static User currentUser;

    private SessionManager() {}

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean hasRole(User.Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }
}