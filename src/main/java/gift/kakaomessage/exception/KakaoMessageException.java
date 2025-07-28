package gift.kakaomessage.exception;

public class KakaoMessageException extends RuntimeException {
    public KakaoMessageException(String message) {
        super(message);
    }

    public KakaoMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}