package Shopping.E_commerce.authrequest;

import Shopping.E_commerce.userRepo.UsersRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component("authService")
public class AuthService {

    private final UsersRepository usersRepository;

    public AuthService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public boolean hasUserId(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) return false;

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return usersRepository.findByUsername(userDetails.getUsername())
                    .map(user -> user.getId().equals(userId))
                    .orElse(false);
        }

        return false;
    }
}
