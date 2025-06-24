package Shopping.E_commerce.userService;

import Shopping.E_commerce.authrequest.UserUpdateRequestDTO;
import Shopping.E_commerce.exception.DuplicateUserException;
import Shopping.E_commerce.userRepo.UsersRepository;
import Shopping.E_commerce.usershops.Role;
import Shopping.E_commerce.usershops.Users;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UsersRepository usersRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Transactional
    public Users registerUser(Users user){
        if (usersRepository.findByUsername(user.getUsername()).isPresent()){
            throw new DuplicateUserException("Username '" + user.getUsername() + "' already exists."

            );
        }if (usersRepository.findByEmail(user.getEmail()).isPresent()){
            throw new DuplicateUserException("Email '" + user.getEmail() + "' already exists.") ;
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
       if (user.getRole() == null){
           user.setRole(Role.USER);
       }
        return usersRepository.save(user);

    }@Transactional(readOnly = true)
    public List<Users>getAllUsers(){

        return usersRepository.findAll();
    }
    @Transactional(readOnly = true)
    public Optional<Users>getUsersById(Long id){

        return usersRepository.findById(id);
    }

    @Transactional
    public Users updateUser(Long id, UserUpdateRequestDTO userUpdateRequest){
        Users users = usersRepository.findById(id)
                .orElseThrow(()-> new UsernameNotFoundException("User not found with id:" + id));
        users.setEmail(userUpdateRequest.getEmail());
        users.setUsername(userUpdateRequest.getUsername());
        if (userUpdateRequest.getPassword() != null && !userUpdateRequest.getPassword().isEmpty()){
            users.setPassword(passwordEncoder.encode(userUpdateRequest.getPassword()));
        }
        users.setRole(userUpdateRequest.getRole());
        return usersRepository.save(users);
    }
    @Transactional
    public void deleteUser(Long id){
        if (!usersRepository.existsById(id)){
            throw new IllegalArgumentException("User not found with id:" + id);

        }
        usersRepository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usersRepository.findByUsername(username).
                orElseThrow(()->new UsernameNotFoundException("User not found with username:" + username));
    }
}
