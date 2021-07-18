package moe.chikalar.bili.interceptor;

import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordRoom;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
public class SplitVideo implements RecordListener {


    // ffmpeg 的 -ss 参数接收一个开始时间，-t或者-fs参数分别指定要截取的时间(s)或者大小(bytes)
    // 如果要按照文件大小去分割的话，需要计算分割出来文件的视频时间，传入下一个命令的-ss参数
    // 暂时不考虑根据文件大小去分割，直接切flv流可能会更容易一些
    // 因为flv的时间戳可能会有问题，分割视频最好强制使用mp4文件，分割视频文件的时候，最好往后多5秒，下一个视频往前5秒，防止出现无画面的情况
    // https://video.stackexchange.com/questions/18284/cutting-with-ffmpeg-results-in-few-seconds-of-black-screen-how-do-i-fix-this

    @Override
    public RecordResult afterRecord(RecordRoom recordRoom, RecordResult recordResult, RecordConfig config) {
        if (config.getConvertToMp4()) {
            String filePath = recordResult.getFilePath();

            File file = new File(filePath);
            if (file.exists()) {
                if (config.getSplitFileByTime()) {
                    Optional<Stream> videoInfo = getVideoInfo(filePath);
                    Optional<Float> durationOpt = videoInfo
                            .map(Stream::getDuration);
                    if (durationOpt.isEmpty()) {
                        return recordResult;
                    }
                    float duration = durationOpt.get();
                    Integer splitFileDurationInSeconds = config.getSplitFileDurationInSeconds();
                    if (duration < splitFileDurationInSeconds) {
                        // 不需要分割视频
                        return recordResult;
                    }

                    for (int starTime = 0, idx = 1; starTime < duration; starTime += splitFileDurationInSeconds, idx++) {
                        String newFileName = filePath.replaceAll("(.*).mp4", "$1-" + idx + ".mp4");
                        // 从第2次开始，开始时间往前挪5s，结束时间往后挪5s
                        FFmpeg.atPath()
                                .setLogLevel(LogLevel.INFO)
                                .setOverwriteOutput(true)
                                .addArguments("-ss", "" + (idx == 1 ? starTime : (starTime - 5)))
                                .addArguments("-i", filePath)
                                .addArguments("-t", "" + (idx == 1 ? splitFileDurationInSeconds : (splitFileDurationInSeconds + 5)))
                                .addArguments("-c", "copy")
                                .addOutput(UrlOutput.toUrl(newFileName))
                                .execute();
                    }


                }

            }
            if (config.getSplitFileBySize()) {
                long sizeInB = file.length();
                long splitByteLength = config.getSplitFileSizeInM() * 1000 * 1000;
                if (sizeInB < splitByteLength) {
                    return recordResult;
                }

                Optional<Stream> durationOpt = getVideoInfo(filePath);
                if (durationOpt.isEmpty()) {
                    return recordResult;
                }
                float duration = durationOpt.get().getDuration();
                float bitRate = durationOpt.get().getBitRate();

                int offsetInSeconds = 5;
//                remainingTime - currentTime <
                for (int starTime = 0, idx = 1; starTime <= duration - offsetInSeconds; idx++) {
                    String newFileName = filePath.replaceAll("(.*).mp4", "$1-" + idx + ".mp4");
                    // 从第2次开始，开始时间往前挪5s，结束时间往后挪5s
                    float splitLength = idx == 1 ? splitByteLength : (splitByteLength + (bitRate * offsetInSeconds / 8));
                    int recordStartTime = idx == 1 ? starTime : (starTime - offsetInSeconds);
                    // 如果现在的开始时间和结束时间相差不大——20s以内，结束时间直接设置为最大
//                    if (duration - starTime == 20) {
//                        splitLength = 3600000000L;
//                    }
                    FFmpeg.atPath()
                            .setLogLevel(LogLevel.INFO)
                            .setOverwriteOutput(true)
                            .addArguments("-ss", "" + recordStartTime)
                            .addArguments("-i", filePath)
                            .addArguments("-fs", "" + splitLength)
                            .addArguments("-c", "copy")
                            .addOutput(UrlOutput.toUrl(newFileName))
                            .execute();
                    // 获取新文件大小

                    Optional<Stream> videoInfo = getVideoInfo(newFileName);
                    if (videoInfo.isEmpty()) {
                        return recordResult;
                    }
                    long currentFileSeconds = videoInfo.get().getDuration().longValue();
                    starTime = (int) (starTime + currentFileSeconds - offsetInSeconds);
                }

            }
            if (config.getSplitFileDeleteSource()) {
                file.delete();
            }
        }
        return recordResult;
    }

    private Optional<Stream> getVideoInfo(String path) {
        FFprobeResult result = FFprobe.atPath()
                .setShowStreams(true)
                .setInput(path)
                .execute();
        return result.getStreams()
                .stream()
                .findFirst();
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }


}
