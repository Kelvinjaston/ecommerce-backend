package Shopping.E_commerce.userController;

import Shopping.E_commerce.userService.CategoryService;
import Shopping.E_commerce.usershops.Category;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category>createCategory(@RequestBody Category category){
        try {
            Category newCategory = categoryService.createCategory(category);
            return new ResponseEntity<>(newCategory, HttpStatus.CREATED);
        }catch (IllegalArgumentException e){
            return new ResponseEntity<>(null,HttpStatus.CONFLICT);
        }
    }
    @GetMapping
    public ResponseEntity<List<Category>>getAllCategories(){
        List<Category>categories = categoryService.getAllCategories();
        return new ResponseEntity<>(categories,HttpStatus.OK);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Category>getCategoryById(@PathVariable Long id){
        return categoryService.getCategoryById(id)
                .map(category -> new ResponseEntity<>(category,HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category>updateCategory(@PathVariable Long id,@RequestBody Category categoryDetails){
        try {
            Category updateCategory = categoryService.updateCategory(id, categoryDetails);
            return new ResponseEntity<>(updateCategory,HttpStatus.OK);
        }catch (IllegalArgumentException e){
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HttpStatus>deleteCategory(@PathVariable Long id){
        try {
            categoryService.deleteCategory(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }catch (IllegalArgumentException e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
