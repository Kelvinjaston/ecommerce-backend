package Shopping.E_commerce.controllerTest;

import Shopping.E_commerce.authrequest.ProductDTO;
import Shopping.E_commerce.securityconfig.jwtconfig.JwtUtil;
import Shopping.E_commerce.userRepo.CategoryRepository;
import Shopping.E_commerce.userRepo.OrderRepository;
import Shopping.E_commerce.userRepo.ProductRepository;
import Shopping.E_commerce.userRepo.UsersRepository;
import Shopping.E_commerce.usershops.Category;
import Shopping.E_commerce.usershops.Product;
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

import java.math.BigDecimal;


import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProductControllerIntegrationTest {
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
    private ProductRepository productRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String userToken;
    private Users adminUser;
    private Users regularUser;
    private Category electronicsCategory;
    private Category clothingCategory;
    private Category musicCategory;
    private Product existingProduct;

    @BeforeEach
    void setUp(){
        orderRepository.deleteAll();;
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        usersRepository.deleteAll();

        adminUser = new Users(null,"adminProductUser","admin.prod@example.com",passwordEncoder.encode("adminPass"), Role.ADMIN);
        adminUser = usersRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser);

        regularUser = new Users(null,"regularProductUser","user.prod@example.com",passwordEncoder.encode("regularPass"),Role.USER);
        regularUser = usersRepository.save(regularUser);
        userToken = jwtUtil.generateToken(regularUser);

        electronicsCategory = new Category(null,"Electronics");
        electronicsCategory = categoryRepository.save(electronicsCategory);

        musicCategory = new Category(null,"Instrument");
        musicCategory = categoryRepository.save(musicCategory);

        clothingCategory = new Category(null,"Clothing");
        clothingCategory = categoryRepository.save(clothingCategory);

        existingProduct = new Product(null,"Laptop","Powerful laptop", BigDecimal.valueOf(1200.00),10,electronicsCategory,"http://example.com/laptop.jpg");
        existingProduct = productRepository.save(existingProduct);
    }
    @Test
    void testCreateProduct_Admin_Success()throws Exception{
        ProductDTO newProd = new ProductDTO(null,"Smartphone","Latest model smartphone",800.00,50,"http://example.com/phone.jpg",electronicsCategory.getId());
        mockMvc.perform(post("/api/products")
                .header("Authorization","Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name",is("Smartphone")))
                .andExpect(jsonPath("$.price",is(800.00)));
        assertThat(productRepository.findByName("Smartphone")).isPresent();
    }
    @Test
    void testCreateProduct_User_Forbidden()throws Exception{
        ProductDTO newProduct = new ProductDTO(null,"T-shirt","Cotton-shirt",25.00,200,"http://example.com/shirt.jpg", electronicsCategory.getId());
        mockMvc.perform(post("/api/products")
                .header("Authorization","Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isForbidden());
        assertThat(productRepository.findByName("T-shirt")).isNotPresent();
    }
    @Test
    void testGetAllProducts_Public_Success()throws Exception{
        productRepository.save(new Product(null,"Keyboard","Mechanical keyboard",BigDecimal.valueOf(100.00),30,musicCategory,"http://example.com/keyboard.jpg"));
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",hasSize(2)))
                .andExpect(jsonPath("$[0].name",is(existingProduct.getName())))
                .andExpect(jsonPath("$[1].name",is("Keyboard")));
    }
    @Test
    void testGetProductById_Public_Success()throws Exception{
        mockMvc.perform(get("/api/products/{id}",existingProduct.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",is(existingProduct.getName())))
                .andExpect(jsonPath("$.price",is(existingProduct.getPrice().doubleValue())));
    }
    @Test
    void testUpdateProduct_Admin_Success()throws Exception{
        ProductDTO updateProd = new ProductDTO(null,"Updated Laptop","Ultra-ligh laptop",1500.00,8,"http://example.com/updated_laptop.jpg",electronicsCategory.getId());
        mockMvc.perform(put("/api/products/{id}",existingProduct.getId())
                .header("Authorization","Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",is("Updated Laptop")))
                .andExpect(jsonPath("$.price",is(1500.00)));
        assertThat(productRepository.findById(existingProduct.getId()).orElseThrow().getName()).isEqualTo("Updated Laptop");
    }
    @Test
    void testUpdateProduct_User_Forbidden()throws Exception{
        ProductDTO updatedproducts =new ProductDTO(null,"Forbidden Product","Attempted update",999.00,1,"http://example.com/updated_laptop.jpg",electronicsCategory.getId());
        mockMvc.perform(put("/api/products/{id}",existingProduct.getId())
                .header("Authorization","Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedproducts)))
                .andExpect(status().isForbidden());
        assertThat(productRepository.findById(existingProduct.getId()).orElseThrow().getName()).isEqualTo(existingProduct.getName());

    }
    @Test
    void testDeleteProduct_Admin_Success()throws Exception{
        mockMvc.perform(delete("/api/products/{id}",existingProduct.getId())
                .header("Authorization","Bearer " + adminToken))
                .andExpect(status().isNoContent());
        assertThat(productRepository.findById(existingProduct.getId())).isNotPresent();
    }
    @Test
    void testDeleteProduct_User_Forbidden()throws Exception{
        mockMvc.perform(delete("/api/products/{id}",existingProduct.getId())
                .header("Authorization","Bearer " + userToken))
                .andExpect(status().isForbidden());
        assertThat(productRepository.findById(existingProduct.getId())).isPresent();
    }



}
