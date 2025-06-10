package Shopping.E_commerce.userController;

import Shopping.E_commerce.userService.UserService;
import Shopping.E_commerce.usershops.Users;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/register")
    public ResponseEntity<Users>registerUsers(@RequestBody Users users){
        try {
            Users registerUsers = userService.registerUser(users);
            return new ResponseEntity<>(registerUsers, HttpStatus.CREATED);
        }catch (IllegalArgumentException e){
            return new ResponseEntity<>(null,HttpStatus.CONFLICT);
        }
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Users>>getAllUsers(){
        List<Users>users = userService.getAllUsers();
        return new ResponseEntity<>(users,HttpStatus.OK);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')or(hasRole('USERS')and @usersServiceImpl.getUsersById(#id).get().username == authentication.name)")
    public ResponseEntity<Users>getUserById(@PathVariable Long id){
        return userService.getUsersById(id).map(users -> new ResponseEntity<>(users,HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @userServiceImpl.getUserById(#id).get().username == authentication.name)")

    public ResponseEntity<Users>updateUser(@PathVariable Long id,@RequestBody Users usersDetails){
        try {
            Users updateUsers = userService.updateUser(id,usersDetails);
            return new ResponseEntity<>(updateUsers,HttpStatus.OK);
        }catch (IllegalArgumentException e){
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HttpStatus>deleteUser(@PathVariable Long id){
        try {
            userService.deleteUser(id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }catch (IllegalArgumentException e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
