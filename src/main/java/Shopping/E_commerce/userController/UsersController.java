package Shopping.E_commerce.userController;

import Shopping.E_commerce.authrequest.UserRegistrationRequestDTO;
import Shopping.E_commerce.authrequest.UserUpdateRequestDTO;
import Shopping.E_commerce.exception.DuplicateUserException;
import Shopping.E_commerce.userService.UserService;
import Shopping.E_commerce.usershops.Role;
import Shopping.E_commerce.usershops.Users;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUsers(@Valid @RequestBody UserRegistrationRequestDTO registrationRequest) {
        try {
            Users newUser = new Users();
            newUser.setUsername(registrationRequest.getUsername());
            newUser.setEmail(registrationRequest.getEmail());
            newUser.setPassword(registrationRequest.getPassword());
            if (registrationRequest.getRole() != null) {
                try {
                    newUser.setRole(Role.valueOf(registrationRequest.getRole().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return new ResponseEntity<>("Invalid role specified: " + registrationRequest.getRole(), HttpStatus.BAD_REQUEST);
                }
            } else {
                newUser.setRole(Role.USER);
            }
            Users registeredUser = userService.registerUser(newUser);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (DuplicateUserException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<>("Error registering user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Users>> getAllUsers() {
        List<Users> users = userService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<Users> getUserById(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = (Users) authentication.getPrincipal();

        if (currentUser.getRole() == Role.ADMIN || currentUser.getId().equals(id)) {
            Optional<Users> user = userService.getUsersById(id);
            return user.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @PutMapping("/{id}")

    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<Users> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequestDTO userUpdateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = (Users) authentication.getPrincipal();

        if (currentUser.getRole() == Role.ADMIN || currentUser.getId().equals(id)) {
            try {
                Users updatedUser = userService.updateUser(id, userUpdateRequest);
                return new ResponseEntity<>(updatedUser, HttpStatus.OK);
            } catch (UsernameNotFoundException e) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
