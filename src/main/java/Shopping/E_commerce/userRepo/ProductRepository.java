package Shopping.E_commerce.userRepo;

import Shopping.E_commerce.usershops.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository <Product,Long> {
    List<Product>findByCategoryId(Long categoryId);
    List<Product>findByNameContainingIgnoreCase(String name);
    List<Product>findByCategory_Name(String categoryName);
    Optional<Product>findByName(String name);
}
