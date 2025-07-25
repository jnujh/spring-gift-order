package gift.service;

import gift.domain.Option;
import gift.domain.Product;
import gift.dto.OptionResponse;
import gift.repository.OptionJpaRepository;
import gift.repository.ProductJpaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class OptionService {

    private final OptionJpaRepository optionRepository;
    private final ProductJpaRepository productRepository;

    public OptionService(OptionJpaRepository optionRepository, ProductJpaRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    // 수량 차감
    @Transactional
    public void subtractQuantity(Long optionId, int quantity) {
        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new NoSuchElementException("해당 옵션을 찾을 수 없습니다."));
        option.subtract(quantity);
    }

    // 특정 상품의 옵션 목록 조회
    @Transactional
    public List<OptionResponse> getOptionsByProductId(Long productId) {
        List<Option> options = optionRepository.findAllByProductId(productId);
        return options.stream()
                .map(OptionResponse::from)
                .toList();
    }

    // 옵션 생성 (옵션 이름 중복 검사 포함)
    @Transactional
    public Long createOption(Long productId, String name, int quantity) {
        if (optionRepository.existsByProductIdAndName(productId, name)) {
            throw new IllegalArgumentException("동일한 이름의 옵션이 이미 존재합니다.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("해당 상품을 찾을 수 없습니다."));

        Option option = Option.create(product, name, quantity);
        optionRepository.save(option);

        return option.getId();
    }

    // 옵션 삭제
    @Transactional
    public void deleteOption(Long optionId) {
        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new NoSuchElementException("해당 옵션이 존재하지 않습니다."));

        Long productId = option.getProduct().getId();
        int count = optionRepository.countByProductId(productId);

        if (count <= 1) {
            throw new IllegalStateException("상품은 최소 1개의 옵션을 유지해야 하므로 삭제할 수 없습니다.");
        }

        optionRepository.deleteById(optionId);
    }

}
