package ma.ensa.khouribga.smartstay.session;

import ma.ensa.khouribga.smartstay.model.User;

import java.util.Objects;
import java.util.Optional;

public final class SessionManager {
    private static volatile User currentUser;

    private SessionManager() {
    }

    public static void setCurrentUser(User user) {
        currentUser = Objects.requireNonNull(user, "user cannot be null");
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static Optional<User> getCurrentUserOptional() {
        return Optional.ofNullable(currentUser);
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }

    public static long getCurrentUserIdOrThrow() {
        User user = currentUser;
        if (user == null) {
            throw new IllegalStateException("No user in session. Please login first.");
        }
        return user.getId();
    }

    public static String getCurrentUsernameOrThrow() {
        User user = currentUser;
        if (user == null) {
            throw new IllegalStateException("No user in session. Please login first.");
        }
        return user.getUsername();
    }

    public static User.Role getCurrentRoleOrThrow() {
        User user = currentUser;
        if (user == null || user.getRole() == null) {
            throw new IllegalStateException("No role in session. Please login again.");
        }
        return user.getRole();
    }

    public static boolean hasRole(User.Role role) {
        return role != null && currentUser != null && currentUser.getRole() == role;
    }

    public static boolean isAdmin() {
        return hasRole(User.Role.ADMIN);
    }

    public static boolean isStaff() {
        return hasRole(User.Role.STAFF);
    }

    public static boolean isClient() {
        return hasRole(User.Role.CLIENT);
    }

    public static void requireLoggedIn() {
        if (!isLoggedIn()) {
            throw new IllegalStateException("Authentication required.");
        }
    }

    public static void requireRole(User.Role role) {
        requireLoggedIn();
        if (!hasRole(role)) {
            throw new SecurityException("Access denied. Required role: " + role);
        }
    }

    public static void requireAnyRole(User.Role... roles) {
        requireLoggedIn();
        if (roles == null || roles.length == 0) {
            throw new IllegalArgumentException("At least one role is required.");
        }
        for (User.Role role : roles) {
            if (hasRole(role)) return;
        }
        throw new SecurityException("Access denied.");
    }
}