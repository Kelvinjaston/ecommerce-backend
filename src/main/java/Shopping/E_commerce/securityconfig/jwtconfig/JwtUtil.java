package Shopping.E_commerce.securityconfig.jwtconfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts; // This is the class that Jwts.parserBuilder() comes from
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct; // Correct for Spring Boot 3+ (Jakarta EE)
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.expiration:86400000}")
    private long EXPIRATION_MILLIS;

    // CRITICAL: Ensure this matches your application.properties/application-test.properties
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private Key signingKey; // This field MUST be present

    @PostConstruct // This method MUST be present and correctly initialized
    public void init() {
        try {
            byte[] decodedKeyBytes = Decoders.BASE64.decode(SECRET_KEY);

            if (decodedKeyBytes.length < 32) { // HS256 requires 32 bytes (256 bits)
                logger.warn("Configured JWT secret key is too short for HS256 algorithm (expected >= 32 bytes, got {} bytes).");
                logger.warn("Generating a new, secure random key for runtime use. PLEASE UPDATE YOUR 'spring.jwt.secret' PROPERTY WITH A STRONGER KEY.");
                generateNewSecureKey();
            } else {
                this.signingKey = Keys.hmacShaKeyFor(decodedKeyBytes);
                logger.info("JWT secret key successfully loaded and validated.");
            }
        } catch (IllegalArgumentException e) {
            logger.error("Configured JWT secret ('{}') is not a valid Base64 string or is corrupted. Error: {}.", SECRET_KEY, e.getMessage());
            logger.error("Generating a new, secure random key for runtime use. PLEASE UPDATE YOUR 'spring.jwt.secret' PROPERTY WITH A VALID BASE64 ENCODED KEY.");
            generateNewSecureKey();
        } catch (Exception e) {
            logger.error("An unexpected error occurred during JWT secret key initialization. Error: {}. Generating a new secure key.", e.getMessage());
            generateNewSecureKey();
        }
    }

    private void generateNewSecureKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 32 bytes for HS256 (256 bits)
        secureRandom.nextBytes(keyBytes);
        SECRET_KEY = Base64.getEncoder().encodeToString(keyBytes); // Update the SECRET_KEY field
        this.signingKey = Keys.hmacShaKeyFor(keyBytes); // Set the internal signingKey
        logger.info("New JWT secret key generated ({} characters): {}", SECRET_KEY.length(), SECRET_KEY);
        logger.warn("Please copy this generated key and update your 'spring.jwt.secret' property in application.properties or application-test.properties to ensure consistent key usage across restarts and deployments.");
    }

    public String extractUsername(String token){
        return extractClaim(token, Claims ::getSubject);
    }

    public Date extractExpiration(String token){
        return extractClaim(token,Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims,T> claimResolver){
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token){
        // Using Jwts.parserBuilder() - This is the modern, non-deprecated method.
        // Your pom.xml's compiler plugin config is designed to help this compile.
        return Jwts.parserBuilder()
                .setSigningKey(this.signingKey) // Use the initialized signingKey field
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails){
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public String generateToken(UserDetails userDetails){
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof Shopping.E_commerce.usershops.Users) {
            claims.put("userId", ((Shopping.E_commerce.usershops.Users)userDetails).getId());
        } else {
            logger.warn("UserDetails object is not an instance of Shopping.E_commerce.usershops.Users. userId claim will not be added to JWT.");
        }
        claims.put("roles", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

        return createToken(claims,userDetails.getUsername());
    }

    private String createToken(Map<String,Object> claims, String subject){
        Date issuedAt = new Date(System.currentTimeMillis());
        Date expiration = new Date(issuedAt.getTime() + EXPIRATION_MILLIS);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(this.signingKey, SignatureAlgorithm.HS256) // Use the initialized signingKey field
                .compact();
    }
}
