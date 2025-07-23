package gift.repository;

import gift.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
class MemberJpaRepositoryTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Test
    @DisplayName("회원을 저장할 수 있다")
    void save() {
        // given - 테스트용 회원 생성
        String email = "test@example.com";
        String password = "Password123!";
        Member member = Member.create(email, password);

        // when - 저장
        Member savedMember = memberJpaRepository.save(member);

        // then - 검증
        assertAll(
                () -> assertThat(savedMember.getId()).isNotNull(),
                () -> assertThat(savedMember.getEmail()).isEqualTo(email),
                () -> assertThat(savedMember.getPassword()).isEqualTo(password)
        );
    }

    @Test
    @DisplayName("이메일로 회원을 찾을 수 있다")
    void findByEmail() {
        // given - 회원 저장
        String email = "test@example.com";
        Member member = Member.create(email, "Password123!");
        memberJpaRepository.save(member);

        // when - 이메일로 조회
        Optional<Member> foundMember = memberJpaRepository.findByEmail(email);

        // then - 검증
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional을 반환한다")
    void findByEmail_notFound() {
        // when - 존재하지 않는 이메일로 조회
        Optional<Member> foundMember = memberJpaRepository.findByEmail("notexist@example.com");

        // then - 빈 Optional 확인
        assertThat(foundMember).isEmpty();
    }

    @Test
    @DisplayName("이메일 존재 여부를 확인할 수 있다")
    void existsByEmail() {
        // given - 회원 저장
        String email = "test@example.com";
        Member member = Member.create(email, "Password123!");
        memberJpaRepository.save(member);

        // when & then - 존재 여부 확인
        assertThat(memberJpaRepository.existsByEmail(email)).isTrue();
        assertThat(memberJpaRepository.existsByEmail("notexist@example.com")).isFalse();
    }

    @Test
    @DisplayName("ID로 회원을 찾을 수 있다")
    void findById() {
        // given - 회원 저장
        Member member = Member.create("test@example.com", "Password123!");
        Member savedMember = memberJpaRepository.save(member);
        Long memberId = savedMember.getId();

        // when - ID로 조회
        Optional<Member> foundMember = memberJpaRepository.findById(memberId);

        // then - 검증
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getId()).isEqualTo(memberId);
    }

    @Test
    @DisplayName("중복 이메일로 회원을 저장하면 예외가 발생한다")
    void save_duplicateEmail() {
        // given - 첫 번째 회원 저장
        String email = "test@example.com";
        Member member1 = Member.create(email, "Password123!");
        memberJpaRepository.save(member1);

        // when & then - 같은 이메일로 두 번째 회원 저장 시도
        Member member2 = Member.create(email, "Password456!");

        assertThatThrownBy(() -> {
            memberJpaRepository.save(member2);
            memberJpaRepository.flush(); // 즉시 DB에 반영하여 제약조건 검증
        }).isInstanceOf(Exception.class);
    }
}