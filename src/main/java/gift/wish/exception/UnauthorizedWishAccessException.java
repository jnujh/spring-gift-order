package gift.global.exception;

public class UnauthorizedWishAccessException extends RuntimeException {
  public UnauthorizedWishAccessException(String message) {
    super(message);
  }
}
