package Shopping.E_commerce.controllerTest;

import Shopping.E_commerce.authrequest.CategoryDTO;
import Shopping.E_commerce.securityconfig.jwtconfig.JwtUtil;
import Shopping.E_commerce.userRepo.CategoryRepository;
import Shopping.E_commerce.userRepo.OrderRepository;
import Shopping.E_commerce.userRepo.UsersRepository;
import Shopping.E_commerce.usershops.Category;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CategoryControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String userToken;
    private Users adminUser;
    private Users regularUser;
    private Category existingCategory;

    @BeforeEach
    void setUp(){
        orderRepository.deleteAll();
        categoryRepository.deleteAll();
        usersRepository.deleteAll();

        adminUser = new Users(null,"adminUser","admin.cat@example.com",passwordEncoder.encode("adminPass"), Role.ADMIN);
        adminUser = usersRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser);

        regularUser = new Users(null,"regularUser","user.cat@example.com",passwordEncoder.encode("userPass"),Role.USER);
        regularUser = usersRepository.save(regularUser);
        userToken = jwtUtil.generateToken(regularUser);

        existingCategory = new Category(null,"Electronics");
        existingCategory = categoryRepository.save(existingCategory);

    }
    @Test
    void testCreateCategory_Admin_Sucess()throws Exception{
        CategoryDTO newCategoryDTO = new CategoryDTO(null,"Books");
        mockMvc.perform(post("/api/categories")
                .header("Authorization","Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCategoryDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name",is("Books")));
        assertThat(categoryRepository.findByName("Books")).isPresent();

    }
    @Test
    void testCreateCategory_User_Forbidden()throws Exception{
        CategoryDTO newCategory = new CategoryDTO(null,"Clothing");
        mockMvc.perform(post("/api/categories")
                .header("Authorization","Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isForbidden());
        assertThat(categoryRepository.findByName("Clothing")).isNotPresent();
    }
    @Test
    void testGetAllCategories_Public_Success()throws Exception{
        categoryRepository.save(new Category(null,"Home Goods"));

        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name",is(existingCategory.getName())))
                .andExpect(jsonPath("$[1].name",is("Home Goods")));
    }
    @Test
    void testGetCategoryById_Public_Success()throws Exception{
        mockMvc.perform(get("/api/categories/{id}",existingCategory.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",is(existingCategory.getId().intValue())))
                .andExpect(jsonPath("$.name",is(existingCategory.getName())));
    }
    @Test
    void testUpdateCategory_Admin_Success()throws Exception{
        CategoryDTO updateCategory = new CategoryDTO(existingCategory.getId(),"Updated Electronics");
        mockMvc.perform(put("/api/categories/{id}",existingCategory.getId())
                .header("Authorization","Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",is("Updated Electronics")));
        assertThat(categoryRepository.findById(existingCategory.getId()).orElseThrow().getName()).isEqualTo("Updated Electronics");
    }
    @Test
    void testUpdateCategory_User_Forbidden()throws Exception{
        CategoryDTO updatedCat= new CategoryDTO(existingCategory.getId(),"Forbidden Update");
        mockMvc.perform(put("/api/categories/{id}",existingCategory.getId())
                .header("Authorization","Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCat)))
                .andExpect(status().isForbidden());
        assertThat(categoryRepository.findById(existingCategory.getId()).orElseThrow().getName()).isEqualTo(existingCategory.getName());
    }
    @Test
    void testDeleteCategory_Admin_Success()throws Exception{
        mockMvc.perform(delete("/api/categories/{id}",existingCategory.getId())
                .header("Authorization","Bearer " + adminToken))
                .andExpect(status().isNoContent());
        assertThat(categoryRepository.findById(existingCategory.getId())).isNotPresent();
    }
    @Test
    void testDeleteCategory_User_Forbidden()throws Exception{
        mockMvc.perform(delete("/api/categories/{id}",existingCategory.getId())
                .header("Authorization","Bearer " + userToken))
                .andExpect(status().isForbidden());
        assertThat(categoryRepository.findById(existingCategory.getId())).isPresent();
    }

}
