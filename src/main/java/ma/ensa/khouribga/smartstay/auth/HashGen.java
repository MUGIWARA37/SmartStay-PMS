package ma.ensa.khouribga.smartstay.auth;

import org.mindrot.jbcrypt.BCrypt;

public class HashGen {
    public static void main(String[] args) {
        System.out.println("admin123  -> " + BCrypt.hashpw("admin123", BCrypt.gensalt()));
        System.out.println("staff123  -> " + BCrypt.hashpw("staff123", BCrypt.gensalt()));
        System.out.println("client123 -> " + BCrypt.hashpw("client123", BCrypt.gensalt()));
    }
}