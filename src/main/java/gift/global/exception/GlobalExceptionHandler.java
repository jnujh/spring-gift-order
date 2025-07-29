package gift.global.exception;

import gift.kakaologin.exception.InvalidKakaoAuthCodeException;
import gift.kakaologin.exception.MismatchedKakaoRedirectUriException;
import gift.kakaomessage.exception.KakaoMessageException;
import gift.order.exception.InsufficientStockException;
import gift.order.exception.OptionNotFoundException;
import gift.wish.exception.AlreadyWishedException;
import gift.wish.exception.UnauthorizedWishAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. @Valid 유효성 검사 실패로 MethodArgumentNotValidException 예외가 터지면 여기서 잡는다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(errors);
    }

    // 2. IllegalArgumentException 예외가 터지면 여기서 잡는다.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());

        return ResponseEntity.badRequest().body(error);
    }

    // 3. IllegalStateException이 발생하면 여기서 잡는다.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException e) {
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 4. 올바르지 않은 이메일, 비밀번호 등 권한이 없어서 재인증해도 안 되는 상태인 경우
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleForbiddenException(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    // 5. 다른 사람의 위시리스트 항목을 삭제하려 하면 터지는 UnauthorizedWishAccessException 예외
    @ExceptionHandler(UnauthorizedWishAccessException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedWishAccessException(UnauthorizedWishAccessException e) {
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // 6. 이미 위시리스트에 존재하는 항목을 추가하려하면 발생하는 AlreadyWishedException 예외
    @ExceptionHandler(AlreadyWishedException.class)
    public ResponseEntity<Map<String, String>> handleAlreadyWishedException(AlreadyWishedException e) {
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 7. 유효하지 않은 카카오 인가 코드로 인한 예외 처리
    @ExceptionHandler(InvalidKakaoAuthCodeException.class)
    public ResponseEntity<Map<String, String>> handleInvalidKakaoAuthCodeException(InvalidKakaoAuthCodeException e) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "카카오 로그인에 실패했습니다. 다시 시도해주세요.");
        // 사용자가 잘못된 인가 코드를 보낸 것이므로, 400 Bad Request를 반환
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 8. 서버에 설정된 Redirect URI와 일치하지 않아 발생하는 예외 처리
    @ExceptionHandler(MismatchedKakaoRedirectUriException.class)
    public ResponseEntity<Map<String, String>> handleMismatchedRedirectUriException(MismatchedKakaoRedirectUriException e) {
        // 개발자의 서버 설정 문제
        Map<String, String> error = new HashMap<>();
        error.put("message", "서버에 오류가 발생했습니다. 관리자에게 문의하세요.");
        // 서버 설정 문제이므로, 500 Internal Server Error를 반환
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // 9. 존재하지 않는 옵션 주문 시 예외 처리
    @ExceptionHandler(OptionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOptionNotFoundException(OptionNotFoundException e) {
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 10. 재고 부족 시 예외 처리
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStockException(InsufficientStockException e) {
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 11. 카카오 메시지 전송 실패 시 예외 처리
    @ExceptionHandler(KakaoMessageException.class)
    public ResponseEntity<Map<String, String>> handleKakaoMessageException(KakaoMessageException e) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "알림 전송 중 서버에 문제가 발생했습니다. 주문은 정상 처리되었을 수 있으니 확인해주세요.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }


    // 위 예외들을 제외하고 다른 예외가 터지면 여기서 잡는다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "오류가 발생했습니다.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }


}
