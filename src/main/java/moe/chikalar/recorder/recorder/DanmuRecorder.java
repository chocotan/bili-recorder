package moe.chikalar.recorder.recorder;

public interface DanmuRecorder {
    default void startRecord(String roomId, String fileName){}

    default void stop(){}
}
