package gift.kakaologin.exception;

public class MismatchedKakaoRedirectUriException extends RuntimeException {
    public MismatchedKakaoRedirectUriException(String message, Throwable cause) {
        super(message, cause);
    }
}
