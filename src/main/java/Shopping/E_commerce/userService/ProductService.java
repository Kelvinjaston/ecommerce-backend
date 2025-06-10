package Shopping.E_commerce.userService;

import Shopping.E_commerce.userRepo.CategoryRepository;
import Shopping.E_commerce.userRepo.ProductRepository;
import Shopping.E_commerce.usershops.Category;
import Shopping.E_commerce.usershops.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }
    @Transactional
    public Product createProduct(Product product) {
        Category category = categoryRepository.findById(product.getCategory().getId()).orElseThrow(() -> new IllegalArgumentException("Category not found with id:" + product.getCategory().getId()));
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
    public  void deleteProduct(Long id){
        if (!productRepository.existsById(id)){
            throw  new IllegalArgumentException("Product not found with id:"+id);
        }
        productRepository.deleteById(id);
    }

}
