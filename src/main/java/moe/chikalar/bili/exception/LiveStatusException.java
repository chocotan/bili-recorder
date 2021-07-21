package moe.chikalar.bili.exception;

public class LiveStatusException extends RuntimeException{
    public LiveStatusException(String message) {
        super(message);
    }

    public LiveStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
