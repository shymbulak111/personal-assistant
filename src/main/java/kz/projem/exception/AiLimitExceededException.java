package kz.projem.exception;

public class AiLimitExceededException extends RuntimeException {

    public AiLimitExceededException(String message) {
        super(message);
    }
}
