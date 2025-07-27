package gift.product.service;

import gift.product.domain.Product;
import gift.product.dto.ProductRequest;
import gift.product.exception.ProductNotFoundException;
import gift.product.repository.ProductJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductJpaRepository  repository;

    public ProductService(ProductJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Product create(ProductRequest request) {
        validateNameContainKakao(request.name());

        Product product = Product.create(
                request.name(),
                request.price(),
                request.imageUrl(),
                request.options()
        );

        return repository.save(product);
    }


    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Product> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return repository.findAll(pageable);
        }

        return repository.findByNameContainingIgnoreCase(keyword, pageable);
    }

    @Transactional
    public void update(Long id, String name, int price, String imageUrl) {
        validateNameContainKakao(name);

        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.update(name, price, imageUrl); // 변경 감지로 업데이트
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            return; // 멱등성 유지
        }
        repository.deleteById(id);
    }

    private void validateNameContainKakao(String name) {
        if (name.contains("카카오")) {
            throw new IllegalArgumentException("'카카오'가 포함된 상품명은 MD와 협의 후 등록 가능합니다.");
        }
    }
}
