package Shopping.E_commerce.service;

import Shopping.E_commerce.authrequest.UserUpdateRequestDTO;
import Shopping.E_commerce.exception.DuplicateUserException;
import Shopping.E_commerce.userRepo.UsersRepository;
import Shopping.E_commerce.userService.UserService;
import Shopping.E_commerce.usershops.Role;
import Shopping.E_commerce.usershops.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    private Users testUser;

    @BeforeEach
    void setUp(){

        testUser = new Users(1L,"testuser","test@example.com","rawPassword", Role.USER);
    }
    @Test
    void testRegisterUser_Success(){
        when(usersRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.empty());
        when(usersRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(usersRepository.save(any(Users.class))).thenReturn(testUser);

        Users registeredUser = userService.registerUser(testUser);
        assertNotNull(registeredUser);
        assertEquals("testuser", registeredUser.getUsername());
        assertEquals("encodedPassword",registeredUser.getPassword());
        assertEquals(Role.USER,registeredUser.getRole());
        verify(usersRepository, times(1)).save(any(Users.class));

    }
    @Test
    void testRegisterUser_UsernameAlreadyTaken(){
        when(usersRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        DuplicateUserException thrown = assertThrows(DuplicateUserException.class, ()->{
            userService.registerUser(testUser);
        });
        assertEquals("Username '" + testUser.getUsername() + "' already exists.", thrown.getMessage());
        verify(usersRepository,never()).save(any(Users.class));
    }
    @Test
    void testRegisterUser_EmailAlreadyTaken(){
        when(usersRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.empty());
        when(usersRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        DuplicateUserException thrown = assertThrows(DuplicateUserException.class,()->{
            userService.registerUser(testUser);
        });
        assertEquals("Email '" + testUser.getEmail() + "' already exists.", thrown.getMessage());
        verify(usersRepository,never()).save(any(Users.class));

    }
    @Test
    void testGetAllUsers(){
        List<Users>users = Arrays.asList(testUser,new Users(2L,"admin","admin@example","pass", Role.ADMIN));
        when(usersRepository.findAll()).thenReturn(users);
        List<Users>fetchUsers = userService.getAllUsers();
        assertNotNull(fetchUsers);
        assertEquals(2,fetchUsers.size());
        verify(usersRepository,times(1)).findAll();
    }
    @Test
    void testGetUserById_Found(){
        when(usersRepository.findById(1L)).thenReturn(Optional.of(testUser));
        Optional<Users> foundUser =userService.getUsersById(1L);
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());

    }
    @Test
    void testGetUserById_NotFound(){
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());
        Optional<Users> foundUser = userService.getUsersById(1L);
        assertFalse(foundUser.isPresent());
    }
    @Test
    void testLoadUserByUsername_Found(){
        when(usersRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        UserDetails userDetails = userService.loadUserByUsername("testuser");
        assertNotNull(userDetails);
        assertEquals("testuser",userDetails.getUsername());
    }
    @Test
    void testLoadUserByUsername_NotFound(){
        when(usersRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,()->{
            userService.loadUserByUsername("nonexistent");
        });
    }
    @Test
    void testUpdateUser_Success(){
        UserUpdateRequestDTO userUpdateRequest = new UserUpdateRequestDTO("updatedUser","updated@example.com","newRawPassword",Role.USER);
        when(usersRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newRawPassword")).thenReturn("newEncodedPassword");
        when(usersRepository.save(any(Users.class))).thenReturn(testUser);
        Users result = userService.updateUser(1L,userUpdateRequest);

        assertNotNull(result);
        assertEquals("updatedUser",result.getUsername());
        assertEquals("updated@example.com",result.getEmail());
        assertEquals("newEncodedPassword",result.getPassword());
        verify(usersRepository,times(1)).save(testUser);
    }
    @Test
    void testDeleteUser_Success(){
        when(usersRepository.existsById(1L)).thenReturn(true);
        doNothing().when(usersRepository).deleteById(1L);
        assertDoesNotThrow(()->userService.deleteUser(1L));
        verify(usersRepository,times(1)).deleteById(1L);
    }
    @Test
    void testDeleteUser_NotFound(){
        when(usersRepository.existsById(1L)).thenReturn(false);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,()->{
            userService.deleteUser(1L);
    });
        assertEquals("User not found with id:" + 1L, thrown.getMessage());
        verify(usersRepository,never()).deleteById(anyLong());
    }
}
