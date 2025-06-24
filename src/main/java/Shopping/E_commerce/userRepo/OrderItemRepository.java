package Shopping.E_commerce.userRepo;

import Shopping.E_commerce.usershops.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {
    List<OrderItem>findByOrderId(Long orderId);
    boolean existsByProductId(Long productId);
}
