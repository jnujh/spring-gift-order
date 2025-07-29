package gift.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 존재하지 않는 옵션
@ResponseStatus(HttpStatus.NOT_FOUND)
public class OptionNotFoundException extends RuntimeException {
  public OptionNotFoundException(String message) {
    super(message);
  }
}