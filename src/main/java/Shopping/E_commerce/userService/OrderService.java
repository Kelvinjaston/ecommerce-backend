package Shopping.E_commerce.userService;

import Shopping.E_commerce.dto.OrderResponseDTO;
import Shopping.E_commerce.dto.OrderItemResponseDTO;
import Shopping.E_commerce.dto.ProductResponseDTO;
import Shopping.E_commerce.dto.UserResponseDTO;
import Shopping.E_commerce.userRepo.OrderItemRepository;
import Shopping.E_commerce.userRepo.OrderRepository;
import Shopping.E_commerce.userRepo.ProductRepository;
import Shopping.E_commerce.userRepo.UsersRepository;
import Shopping.E_commerce.usershops.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UsersRepository usersRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, UsersRepository usersRepository, OrderItemRepository orderItemRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.usersRepository = usersRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    private OrderResponseDTO convertToDto(Order order) {
        if (order == null) {
            return null;
        }

        UserResponseDTO userDTO = null;
        if (order.getUsers() != null) {
            userDTO = new UserResponseDTO(
                    order.getUsers().getId(),
                    order.getUsers().getUsername(),
                    order.getUsers().getEmail(),
                    order.getUsers().getRole()
            );
        }

        List<OrderItemResponseDTO> orderItemDTOs = new ArrayList<>();
        if (order.getOrderItems() != null) {
            orderItemDTOs = order.getOrderItems().stream().map(orderItem -> {
                Product product = orderItem.getProduct();
                ProductResponseDTO productDTO = null;
                if (product != null) {
                    productDTO = new ProductResponseDTO(
                            product.getId(),
                            product.getName(),
                            product.getDescription(),
                            product.getPrice(),
                            product.getImageUrl()
                    );
                }
                return new OrderItemResponseDTO(
                        orderItem.getId(),
                        productDTO,
                        orderItem.getQuantity(),
                        orderItem.getUnitPrice()
                );
            }).collect(Collectors.toList());
        }

        return new OrderResponseDTO(
                order.getId(),
                userDTO,
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getStatus(),
                orderItemDTOs
        );
    }

    @Transactional
    public Order placeOrder(Long userId, List<OrderItem> items) {
        Users users = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Order order = new Order();
        order.setUsers(users);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItem itemRequest : items) {
            Product product = productRepository.findById(itemRequest.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + itemRequest.getProduct().getId()));

            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException("Not enough stock for product: " + product.getName() + ". Available: " + product.getStockQuantity() + ", Requested: " + itemRequest.getQuantity());
            }

            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            productRepository.save(product);

            OrderItem newOrderItem = new OrderItem();
            newOrderItem.setOrder(order);
            newOrderItem.setProduct(product);
            newOrderItem.setQuantity(itemRequest.getQuantity());
            newOrderItem.setUnitPrice(product.getPrice());

            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(newOrderItem.getQuantity())));
            orderItems.add(newOrderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAllWithDetails();
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponseDTO> getOrderById(Long id) {
        Optional<Order> orderOptional = orderRepository.findByIdWithDetails(id);
        return orderOptional.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUsersIdWithDetails(userId);
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus newStatus){
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        return convertToDto(updatedOrder);
    }
}
