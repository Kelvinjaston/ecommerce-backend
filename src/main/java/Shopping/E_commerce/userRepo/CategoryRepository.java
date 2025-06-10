package Shopping.E_commerce.userRepo;

import Shopping.E_commerce.usershops.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category,Long> {
    Optional<Category>findByName(String name);
}
