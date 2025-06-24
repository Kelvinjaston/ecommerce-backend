package Shopping.E_commerce.authrequest;

import Shopping.E_commerce.usershops.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name ;
    private String description;
    private Double price ;
    private Integer stockQuantity;
    private String imageUrl;
    private Long categoryId;
}
