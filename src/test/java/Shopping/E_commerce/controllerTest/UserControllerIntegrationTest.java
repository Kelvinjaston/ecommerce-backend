package Shopping.E_commerce.controllerTest;

import Shopping.E_commerce.authrequest.UserUpdateRequestDTO;
import Shopping.E_commerce.securityconfig.jwtconfig.JwtUtil;
import Shopping.E_commerce.userRepo.OrderRepository;
import Shopping.E_commerce.userRepo.UsersRepository;
import Shopping.E_commerce.usershops.Role;
import Shopping.E_commerce.usershops.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import Shopping.E_commerce.authrequest.UserRegistrationRequestDTO;
import Shopping.E_commerce.authrequest.AuthRequest;


import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String userToken;
    private Users adminUser;
    private Users regularUser;

    @BeforeEach
    void setUp(){
        orderRepository.deleteAll();
        usersRepository.deleteAll();
        adminUser = new Users(null, "adminUser", "admin@example.com", passwordEncoder.encode("adminPass"), Role.ADMIN);
        adminUser = usersRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser);

        regularUser = new Users(null, "regularUser", "user@example.com", passwordEncoder.encode("userPass"),Role.USER);
        regularUser = usersRepository.save(regularUser);
        userToken = jwtUtil.generateToken(regularUser);
    }
    @Test
    void testRegisterUser_Success() throws Exception{
        UserRegistrationRequestDTO registrationRequest= new UserRegistrationRequestDTO("newUser", "new@example.com", "newPass", "USER");
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("newUser")))
                .andExpect(jsonPath("$.email",is("new@example.com")));
        assertThat(usersRepository.findByUsername("newUser")).isPresent();
    }
    @Test
    void testRegisterUser_DuplicateUsername()throws Exception{
        UserRegistrationRequestDTO duplicateUserRequest = new UserRegistrationRequestDTO("adminUser","another@example.com","validPass","USER");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUserRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Username 'adminUser' already exists."));
    }
    @Test
    void testGetAllUsers_Admin_Success()throws Exception{
        mockMvc.perform(get("/api/users")
                .header("Authorization","Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",hasSize(2)))
                .andExpect(jsonPath("$[0].username",is("adminUser")))
                .andExpect(jsonPath("$[1].username",is("regularUser")));
    }
    @Test
    void testGetAllUsers_User_Forbidden()throws Exception{
        mockMvc.perform(get("/api/users")
                .header("Authorization","Bearer " + userToken))
                .andExpect(status().isForbidden());

    }
    @Test
    void testGetUserById_Admin_Success()throws Exception{
        mockMvc.perform(get("/api/users/{id}", regularUser.getId())
                .header("Authorization","Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username",is(regularUser.getUsername())));
    }
    @Test
    void testGetUserById_User_OwnProfile_Success()throws Exception{
        mockMvc.perform(get("/api/users/{id}",regularUser.getId())
                .header("Authorization","Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username",is(regularUser.getUsername())));
    }
    @Test
    void testGetUserById_User_OtherProfile_Forbidden()throws Exception{
        mockMvc.perform(get("/api/users/{id}",adminUser.getId())
                .header("Authorization","Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
    @Test
    void testUpdateUser_Admin_Success()throws Exception{
        UserUpdateRequestDTO userUpdateRequest = new UserUpdateRequestDTO("updatedAdmin","updated.admin@example.com","newAdminPass",Role.ADMIN);
        mockMvc.perform(put("/api/users/{id}",adminUser.getId())
                .header("Authorization","Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username",is("updatedAdmin")))
                .andExpect(jsonPath("$.email",is("updated.admin@example.com")));
        Users retrieveUser = usersRepository.findById(adminUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newAdminPass",retrieveUser.getPassword())).isTrue();
    }
    @Test
    void testUpdateUser_User_OwnProfile_Success()throws Exception{
        UserUpdateRequestDTO userUpdateRequest = new UserUpdateRequestDTO("updatedUser","updated.user@example.com","newPass",Role.USER);
        mockMvc.perform(put("/api/users/{id}",regularUser.getId())
                .header("Authorization","Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username",is("updatedUser")))
                .andExpect(jsonPath("$.email",is("updated.user@example.com")));
    }
    @Test
    void testUpdatedUser_User_OtherProfile_Forbidden()throws Exception{
        UserUpdateRequestDTO userUpdateRequest =new UserUpdateRequestDTO("maliciousUpdate","mal@example.com","passWord",Role.USER);
        mockMvc.perform(put("/api/users/{id}",adminUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString("updatedDetails")))
                .andExpect(status().isForbidden());

    }
    @Test
    void testDeleteUser_Admin_Success()throws Exception{
        mockMvc.perform(delete("/api/users/{id}",regularUser.getId())
                .header("Authorization","Bearer " + adminToken))
                .andExpect(status().isNoContent());
        assertThat(usersRepository.existsById(regularUser.getId())).isFalse();
    }
    @Test
    void testDeleteUser_User_Forbidden()throws Exception{
        mockMvc.perform(delete("/api/users/{id}",adminUser.getId())
                .header("Authorization","Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

}
