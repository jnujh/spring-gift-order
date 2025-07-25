package gift.wish.exception;

public class AlreadyWishedException extends RuntimeException {
  public AlreadyWishedException() {
    super("이미 찜한 상품입니다.");
  }
}
