package gift.repository;

import gift.domain.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionJpaRepository extends JpaRepository<Option, Long> {

    // 특정 상품의 옵션 목록 조회
    List<Option> findAllByProductId(Long productId);

    // 옵션 이름 존재 여부 조회
    boolean existsByProductIdAndName(Long productId, String name);

    // 옵션 개수 조회
    int countByProductId(Long productId);
}
