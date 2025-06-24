package Shopping.E_commerce.controllerTest;

import Shopping.E_commerce.authrequest.OrderItemDTO;
import Shopping.E_commerce.authrequest.OrderRequestDTO;
import Shopping.E_commerce.authrequest.OrderUpdateStatusDTO;
import Shopping.E_commerce.securityconfig.jwtconfig.JwtUtil;
import Shopping.E_commerce.userRepo.CategoryRepository;
import Shopping.E_commerce.userRepo.OrderRepository;
import Shopping.E_commerce.userRepo.ProductRepository;
import Shopping.E_commerce.userRepo.UsersRepository;
import Shopping.E_commerce.usershops.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static Shopping.E_commerce.usershops.OrderStatus.PENDING;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class OrderControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private OrderRepository orderRepository;


    private Users adminUser;
    private Users regularUser;
    private String adminToken;
    private String userToken;
    private Category testCategory;
    private Product testProduct1;
    private Product testProduct2;
    private Order existingOrder;

    @BeforeEach
    void setUp(){
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        usersRepository.deleteAll();

        adminUser = new Users(null,"adminOrderUser","admin.order@example.com",passwordEncoder.encode("orderPass"), Role.ADMIN);
        adminUser = usersRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser);

        regularUser = new Users(null,"regOrderUser","reg.order@example.com",passwordEncoder.encode("regPass"),Role.USER);
        regularUser = usersRepository.save(regularUser);
        userToken = jwtUtil.generateToken(regularUser);

            OrderStatus status = PENDING;
            List<OrderItem>orderItems = Collections.emptyList();

        testCategory = new Category(null,"Test Category for Order");
        testCategory = categoryRepository.save(testCategory);

        testProduct1 = new Product(null,"Test Product 1","Desc 1", BigDecimal.valueOf(100.00),5,testCategory,"url1");
        testProduct1 = productRepository.save(testProduct1);

        testProduct2 = new Product(null,"Test Product 2","Desc 2",BigDecimal.valueOf(50.00),10,testCategory,"url2");
        testProduct2 = productRepository.save(testProduct2);

        existingOrder = new Order(null,regularUser, LocalDateTime.now(),BigDecimal.valueOf(150.00),PENDING,orderItems);

        OrderItem item1 = new OrderItem(null,existingOrder,testProduct1,1,BigDecimal.valueOf(100.00));
        OrderItem item2 = new OrderItem(null,existingOrder,testProduct2,1,BigDecimal.valueOf(50.00));

        existingOrder = orderRepository.save(existingOrder);

    }
    @Test
    void testCreateOrder_User_Success()throws Exception{
        OrderItemDTO item1 = new OrderItemDTO(testProduct1.getId(),2,testProduct1.getPrice().doubleValue());
        OrderItemDTO item2 = new OrderItemDTO(testProduct2.getId(),1,testProduct2.getPrice().doubleValue());

        List<OrderItemDTO> items = Arrays.asList(item1, item2);

        int initialQty1 = testProduct1.getStockQuantity();
        int initialQty2 = testProduct2.getStockQuantity();

        mockMvc.perform(post("/api/orders/place/{id}",regularUser.getId())
                .header("Authorization","Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(items)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",notNullValue()))
                .andExpect(jsonPath("$.status",is(PENDING.name())))
                .andExpect(jsonPath("$.totalAmount",is(2 * testProduct1.getPrice().doubleValue() +testProduct2.getPrice().doubleValue())))
                .andExpect(jsonPath("$.orderItems",hasSize(2)));
         Product updateProduct1 = productRepository.findById(testProduct1.getId()).orElseThrow();
         Product updateProduct2 = productRepository.findById(testProduct2.getId()).orElseThrow();

        assertThat(updateProduct1.getStockQuantity()).isEqualTo(initialQty1 - 2);
        assertThat(updateProduct2.getStockQuantity()).isEqualTo(initialQty2 - 1);

    }
    @Test
    void testCreateOrder_Unauthorized()throws Exception{
        OrderItemDTO item1 = new OrderItemDTO(testProduct1.getId(),1,testProduct1.getPrice().doubleValue());
        OrderRequestDTO orderRequest = new OrderRequestDTO(Collections.singletonList(item1));

        mockMvc.perform(post("/api/orders/place/{id}",regularUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isForbidden());
    }
    @Test
    void testGetAllOrders_Admin_Success()throws Exception{
        Order adminOrder = new Order(null,adminUser,LocalDateTime.now(),BigDecimal.valueOf(20.00),OrderStatus.DELIVERED,Collections.emptyList());
       adminOrder= orderRepository.save(adminOrder);

       mockMvc.perform(get("/api/orders")
               .header("Authorization","Bearer " + adminToken)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(adminOrder)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$",hasSize(2)))
               .andExpect(jsonPath("$[0].status",is(existingOrder.getStatus().name())))
               .andExpect(jsonPath("$[1].status",is(adminOrder.getStatus().name())));
    }
    @Test
    void testGetAllOrders_User_Forbidden()throws Exception{
        mockMvc.perform(get("/api/orders")
                .header("Authorization","Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
    @Test
    void testGetOwnerOrders_User_Success()throws Exception{
        mockMvc.perform(get("/api/orders/user/{userId}",regularUser.getId())
                .header("Authorization","Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",hasSize(1)))
                .andExpect(jsonPath("$[0].id",is(existingOrder.getId().intValue())))
                .andExpect(jsonPath("$[0].user.username",is(regularUser.getUsername())));
    }
    @Test
    void testGetOrderById_User_OwnOrder_Success()throws Exception{
        mockMvc.perform(get("/api/orders/{id}",existingOrder.getId())
                .header("Authorization","Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",is(existingOrder.getId().intValue())))
                .andExpect(jsonPath("$.user.username",is(regularUser.getUsername())));
    }
    @Test
    void testGetOrderById_Admin_Success()throws Exception{
        mockMvc.perform(get("/api/orders/{id}",existingOrder.getId())
                .header("Authorization","Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",is(existingOrder.getId().intValue())))
                .andExpect(jsonPath("$.user.username",is(regularUser.getUsername())));
    }
    @Test
    void testGetOrderById_User_OtherOrder_Forbidden()throws Exception{
        Order anotherOrder = new Order(null,adminUser,LocalDateTime.now(),BigDecimal.valueOf(99.99), PENDING,Collections.emptyList());
        anotherOrder = orderRepository.save(anotherOrder);

        mockMvc.perform(get("/api/orders/{id}",anotherOrder.getId())
                .header("Authorization","Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
    @Test
    void testUpdateOrderStatus_Admin_Success()throws Exception{
        OrderUpdateStatusDTO updateStatus = new OrderUpdateStatusDTO(OrderStatus.SHIPPED.name());

        mockMvc.perform(put("/api/orders/{id}/status",existingOrder.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status",is(OrderStatus.SHIPPED.name())));
        assertThat(orderRepository.findById(existingOrder.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }
    @Test
    void testUpdateOrderStatus_User_Forbidden()throws Exception{
        OrderUpdateStatusDTO updateStatusDTO = new OrderUpdateStatusDTO(OrderStatus.DELIVERED.name());

        mockMvc.perform(put("/api/orders/{id}/status",existingOrder.getId())
                .header("Authorization","Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateStatusDTO)))
                .andExpect(status().isForbidden());
        assertThat(orderRepository.findById(existingOrder.getId()).orElseThrow().getStatus()).isEqualTo(existingOrder.getStatus());
    }
    @Test
    void testDeleteOrder_Admin_Success()throws Exception{
        mockMvc.perform(delete("/api/orders/{id}",existingOrder.getId())
                .header("Authorization","Bearer " + adminToken))
                .andExpect(status().isNoContent());
        assertThat(orderRepository.findById(existingOrder.getId())).isNotPresent();
    }
    @Test
    void testDeleteOrder_User_Forbidden()throws Exception{
        mockMvc.perform(delete("/api/orders/{id}",existingOrder.getId())
                .header("Authorization","Bearer " + userToken))
                .andExpect(status().isForbidden());
        assertThat(orderRepository.findById(existingOrder.getId())).isPresent();
    }

}
