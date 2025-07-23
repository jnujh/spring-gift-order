package gift.controller;

import gift.dto.AuthResponse;
import gift.dto.MemberRequest;
import gift.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid MemberRequest request) {
        String token = memberService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthResponse.of(token));
    }

    // 로그인
    // 로그인은 토큰을 생성하기도 하고 GET 요청은 파라미터에 비밀번호 노출 위험이 있기 때문에 POST 요청 사용
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid MemberRequest request) {
        String token = memberService.login(request.email(), request.password());
        return ResponseEntity.ok(AuthResponse.of(token));
    }
}