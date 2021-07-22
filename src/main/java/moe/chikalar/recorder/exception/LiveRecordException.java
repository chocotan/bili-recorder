package moe.chikalar.recorder.exception;

public class LiveRecordException extends RuntimeException{
    public LiveRecordException(String message) {
        super(message);
    }

    public LiveRecordException(Throwable cause) {
        super(cause);
    }
}
