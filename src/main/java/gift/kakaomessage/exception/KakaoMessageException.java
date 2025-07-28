// 신규 파일: src/main/java/gift/kakaomessage/exception/KakaoMessageException.java
package gift.kakaomessage.exception;

public class KakaoMessageException extends RuntimeException {
    public KakaoMessageException(String message) {
        super(message);
    }

    public KakaoMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}