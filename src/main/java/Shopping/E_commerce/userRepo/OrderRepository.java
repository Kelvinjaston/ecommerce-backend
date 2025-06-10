package Shopping.E_commerce.userRepo;

import Shopping.E_commerce.usershops.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Eagerly fetch users, orderItems, AND product details for all orders
    // DISTINCT is used to prevent duplicate Order objects if an order has multiple order items.
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.users u LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product p")
    List<Order> findAllWithDetails();

    // Eagerly fetch users, orderItems, AND product details for a single order by ID
    @Query("SELECT o FROM Order o JOIN FETCH o.users u LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product p WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(Long id);

    // Eagerly fetch users, orderItems, AND product details for orders by user ID
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.users u LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product p WHERE u.id = :userId")
    List<Order> findByUsersIdWithDetails(Long userId);

    // Keep default findById for simple cases where associated data isn't always needed,
    // or if you want to explicitly convert to DTO without fetching everything (less common now).
    Optional<Order> findById(Long id);

    // This method is now redundant because findByUsersIdWithDetails is more comprehensive.
    // However, if you have other parts of your code directly calling findByUsersId, you can keep it.
    // List<Order> findByUsersId(Long userId);
}
