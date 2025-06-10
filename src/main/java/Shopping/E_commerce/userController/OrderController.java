package Shopping.E_commerce.userController;

import Shopping.E_commerce.dto.OrderResponseDTO;
import Shopping.E_commerce.userService.OrderService;
import Shopping.E_commerce.usershops.Order;
import Shopping.E_commerce.usershops.OrderItem;
import Shopping.E_commerce.usershops.OrderStatus;
import Shopping.E_commerce.usershops.Users;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/place/{userId}")
    @PreAuthorize("hasAuthority('ROLE_USER') and #userId == authentication.principal.id")
    public ResponseEntity<Order>placeOrder(@PathVariable Long userId, @RequestBody List<OrderItem>items){
        try {
            Order newOrder = orderService.placeOrder(userId, items);
            return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
        }catch (IllegalArgumentException e){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<OrderResponseDTO>>getAllOrders(){
        List<OrderResponseDTO>orders = orderService.getAllOrders();
        return new ResponseEntity<>(orders,HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or (hasAuthority('ROLE_USER') and @orderService.getOrderById(#id).isPresent() and @orderService.getOrderById(#id).get().user.id == ((Shopping.E_commerce.usershops.Users) authentication.principal).getId())")
    public ResponseEntity<OrderResponseDTO>getOrderById(@PathVariable Long id){ // Changed return type to OrderResponseDTO
        Optional<OrderResponseDTO> orderOptional = orderService.getOrderById(id); // Call method returning Optional<OrderResponseDTO>
        return orderOptional
                .map(orderDTO -> new ResponseEntity<>(orderDTO,HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or (hasAuthority('ROLE_USER') and #userId == authentication.principal.id)")
    public ResponseEntity<List<OrderResponseDTO>>getOrdersByUserId(@PathVariable Long userId){
        List<OrderResponseDTO>orders = orderService.getOrdersByUserId(userId);
        return new ResponseEntity<>(orders,HttpStatus.OK);
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OrderResponseDTO>updateOrderStatus(@PathVariable Long orderId, @RequestBody Map<String, String>statusUpdate){
        try {
            OrderStatus newStatus =OrderStatus.valueOf(statusUpdate.get("status").toUpperCase());
            OrderResponseDTO updatedOrderDTO = orderService.updateOrderStatus(orderId,newStatus);
            return new ResponseEntity<>(updatedOrderDTO,HttpStatus.OK);
        }catch (IllegalArgumentException e){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
    }
}
