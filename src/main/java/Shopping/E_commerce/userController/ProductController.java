package Shopping.E_commerce.userController;

import Shopping.E_commerce.authrequest.ProductDTO;
import Shopping.E_commerce.exception.ProductDeletionException;
import Shopping.E_commerce.userService.ProductService;
import Shopping.E_commerce.usershops.Product;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {

        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> createProduct(@RequestBody ProductDTO productDTO) {
        try {
            Product newProduct = productService.createProduct(productDTO);
            return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProduct() {
        List<Product> products = productService.getAllProduct();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
            return productService.getProductById(id)
                    .map(product -> new ResponseEntity<>(product, HttpStatus.OK))
                    .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>>getProductByCategoryId(@PathVariable Long categoryId){
        List<Product>products = productService.getProductByCategoryId(categoryId);
        return new ResponseEntity<>(products,HttpStatus.OK);
    }
    @GetMapping("/search")
    public ResponseEntity<List<Product>>searchProducts(@RequestParam String keyword){
        List<Product>products = productService.searchProducts(keyword);
        return new ResponseEntity<>(products,HttpStatus.OK);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product>updateProduct(@PathVariable Long id, @RequestBody Product productDetails){
        try {
            Product updateProduct = productService.updateProduct(id,productDetails);
            return new ResponseEntity<>(updateProduct,HttpStatus.OK);
        }catch (IllegalArgumentException e){
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String>deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return new ResponseEntity<>("Product with ID " + id + " deleted successfully.", HttpStatus.NO_CONTENT);
        } catch (ProductDeletionException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<>("An unexpected error occurred during product deletion.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
