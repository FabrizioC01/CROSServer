package Errors;

public class UnknownJsonObject extends RuntimeException {
  public UnknownJsonObject(String message) {
    super(message);
  }
}
