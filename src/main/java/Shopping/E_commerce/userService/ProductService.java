package Shopping.E_commerce.userService;

import Shopping.E_commerce.authrequest.ProductDTO;
import Shopping.E_commerce.exception.ProductDeletionException;
import Shopping.E_commerce.userRepo.CategoryRepository;
import Shopping.E_commerce.userRepo.OrderItemRepository;
import Shopping.E_commerce.userRepo.ProductRepository;
import Shopping.E_commerce.usershops.Category;
import Shopping.E_commerce.usershops.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    OrderItemRepository orderItemRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository =orderItemRepository;
    }
    @Transactional
    public Product createProduct(ProductDTO productDTO) {
        Category category = categoryRepository.findById(productDTO.getCategoryId()).orElseThrow(() -> new IllegalArgumentException("Category not found with id:" + productDTO.getCategoryId()));
        productDTO.getCategoryId();

        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(BigDecimal.valueOf(productDTO.getPrice()));
        product.setStockQuantity(productDTO.getStockQuantity());
        product.setImageUrl(productDTO.getImageUrl());
        product.setCategory(category);

        return productRepository.save(product);
    }
    @Transactional(readOnly = true)
    public List<Product> getAllProduct(){
        return productRepository.findAll();
    }
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id){
        return productRepository.findById(id);
    }
    @Transactional(readOnly = true)
    public List<Product> getProductByCategoryId(Long categoryId){
        return productRepository.findByCategoryId(categoryId);
    }
    @Transactional(readOnly = true)
    public List<Product>searchProducts(String keyword){
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }
    @Transactional
    public Product updateProduct(Long id,Product productDetails){
        Product product = productRepository.findById(id).orElseThrow(()-> new IllegalArgumentException("Product not found with id:" + id));
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setImageUrl(productDetails.getImageUrl());

        if (productDetails.getCategory()!=null&&productDetails.getCategory().getId()!=null){
            Category newCategory = categoryRepository.findById(productDetails.getCategory().getId())
                    .orElseThrow(()->new IllegalArgumentException("New category not found with id:" + productDetails.getCategory().getId()));
            product.setCategory(newCategory);
        }
        return productRepository.save(product);
    }
    @Transactional
    public void deleteProduct(Long productId) {
        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            throw new ProductDeletionException("Product with ID " + productId + " not found.");
        }


        if (orderItemRepository.existsByProductId(productId)) {
            throw new ProductDeletionException("Cannot delete product with ID " + productId + " as it is linked to existing order items.");
        }
        try {
            productRepository.deleteById(productId);
        } catch (DataIntegrityViolationException ex) {
            throw new ProductDeletionException("Failed to delete product due to data integrity issues. It might be linked to other records.", ex);
        }
    }


}
