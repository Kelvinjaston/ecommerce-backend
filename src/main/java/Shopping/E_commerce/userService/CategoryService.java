package Shopping.E_commerce.userService;

import Shopping.E_commerce.userRepo.CategoryRepository;
import Shopping.E_commerce.usershops.Category;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    @Transactional
    public Category createCategory(Category category) {
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            throw new IllegalArgumentException("Category with name '" + category.getName()
                    + "' already exists");
        }
        return categoryRepository.save(category);

    }
    @Transactional(readOnly = true)
    public List<Category>getAllCategories(){

        return categoryRepository.findAll();
    }
    @Transactional(readOnly = true)
    public Optional<Category>getCategoryById(Long id){

        return categoryRepository.findById(id);
    }
    @Transactional
    public Category updateCategory(Long id,Category categoryDetails){
        Category category = categoryRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("Category not found with id: " + id));
        if (categoryRepository.findByName(categoryDetails.getName()).filter(c->!c.getId().equals(id)).isPresent()){
            throw new IllegalArgumentException("Category with name '" + categoryDetails.getName()+"' already exists ");
        }
        category.setName(categoryDetails.getName());
        return categoryRepository.save(category);
    }
    @Transactional
    public void deleteCategory(Long id){
        if (!categoryRepository.existsById(id)){
            throw new IllegalArgumentException("Category not found with id:" + id);
        }
        categoryRepository.deleteById(id);

    }

}

