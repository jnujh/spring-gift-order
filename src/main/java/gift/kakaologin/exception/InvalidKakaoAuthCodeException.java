package gift.kakaologin.exception;

public class InvalidKakaoAuthCodeException extends RuntimeException {
    public InvalidKakaoAuthCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
