package fr.anisekai.media.bin.wrapper.tasks;

import fr.anisekai.media.MediaMeta;
import fr.anisekai.media.bin.Binary;
import fr.anisekai.media.bin.wrapper.FFMpegCommand;
import fr.anisekai.media.bin.wrapper.FFMpegCommandTask;
import fr.anisekai.media.enums.CodecType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specific implementation {@link FFMpegCommand} allowing to combine multiple {@link MediaMeta} into a single file
 * represented by a {@link Path}.
 */
public class CombineTask extends FFMpegCommandTask<Path> {

    private final List<MediaMeta> mediaMetaCollection;
    private final Path            outputFile;

    /**
     * Create a new {@link CombineTask} instance.
     *
     * @param mediaMetaCollection
     *         All the {@link MediaMeta} to combine
     * @param outputFile
     *         The {@link Path} pointing to the file that will be created.
     */
    public CombineTask(List<MediaMeta> mediaMetaCollection, Path outputFile) {

        super(Binary.ffmpeg());
        this.mediaMetaCollection = mediaMetaCollection;
        this.outputFile          = outputFile;
    }

    @Override
    public void preprocess(Binary ffmpeg) throws IOException {

        ffmpeg.setBaseDir(this.outputFile.getParent());

        Map<CodecType, Integer> streamTypes = new HashMap<>();
        streamTypes.put(CodecType.VIDEO, 1);
        streamTypes.put(CodecType.AUDIO, 1);
        streamTypes.put(CodecType.SUBTITLE, 1);

        for (int i = 0; i < this.mediaMetaCollection.size(); i++) {
            MediaMeta mediaMeta = this.mediaMetaCollection.get(i);
            CodecType type      = mediaMeta.getCodecType();

            streamTypes.compute(type, (k, trackTypeNum) -> trackTypeNum + 1);

            ffmpeg.addArguments("-i", mediaMeta.getPath().toString());
            ffmpeg.addHoldArguments("-map", "%s:%s".formatted(i, type.getChar()));

            for (String key : mediaMeta.getMetadata().keySet()) {
                String value = mediaMeta.getMetadata().get(key);
                ffmpeg.addHoldArguments("-metadata:s:%s".formatted(i), "%s=%s".formatted(key, value));
            }
        }

        ffmpeg.commitHoldArguments();

        ffmpeg.addArguments("-c", "copy");
        ffmpeg.addArguments(this.outputFile.toString());

        Files.deleteIfExists(this.outputFile);
    }

    @Override
    public Path postprocess(int code) {

        return this.outputFile;
    }

}
