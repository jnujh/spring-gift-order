package gift.wish.repository;

import gift.wish.domain.Wish;
import gift.member.domain.Member;
import gift.product.domain.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

@Repository
public interface WishJpaRepository extends JpaRepository<Wish, Long> {

    // 중복 찜 여부 확인
    boolean existsByMemberAndProduct(Member member, Product product);

    // 사용자 ID 에 해당하는 찜 목록 전체 조회 (WishId 기준 내림차순 정렬)
    // List<Wish> findAllByMemberIdAndOrderByIdDesc(Member member);

    // Wish ID + 사용자 ID 기준 삭제
    @Modifying
    @Transactional
    @Query("delete from Wish w where w.id = :id and w.member.id = :memberId")
    void deleteByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);

    // 사용자 ID 소유의 찜 항목 조회
    Optional<Wish> findByIdAndMemberId(Long id, Long memberId);

    // 페이지네이션
    Page<Wish> findByMemberId(Long memberId, Pageable pageable);

    void deleteByMemberAndProduct(Member member, Product product);
}
