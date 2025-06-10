package Shopping.E_commerce.securityconfig.jwtconfig;

import java.security.SecureRandom;
import java.util.Base64;

public class KeyGenerator {
        public static void main(String[] args) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] keyBytes = new byte[32]; // For HS256, 32 bytes = 256 bits
            secureRandom.nextBytes(keyBytes);
            String encodedKey = Base64.getEncoder().encodeToString(keyBytes);
            System.out.println("Generated Secret Key: " + encodedKey);
        }
}
