package moe.chikalar.bili.recorder;

public interface DanmuRecorder {
    default void startRecord(String roomId, String fileName){}

    default void stop(){}
}
