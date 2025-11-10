package fr.anisekai.media.bin.wrapper.tasks;

import fr.anisekai.media.MediaFile;
import fr.anisekai.media.MediaStream;
import fr.anisekai.media.bin.Binary;
import fr.anisekai.media.bin.wrapper.FFMpegCommand;
import fr.anisekai.media.bin.wrapper.FFMpegCommandTask;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.interfaces.MediaStreamMapper;
import fr.anisekai.media.interfaces.MediaStreamNamer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Specific implementation {@link FFMpegCommand} allowing to convert a {@link MediaFile} into a single file or split its
 * stream into multiple files.
 *
 * @param <T>
 *         The type of the result for this {@link ConvertTask}.
 */
public final class ConvertTask<T> extends FFMpegCommandTask<T> {

    /**
     * Create a {@link ConvertTask} that will convert a {@link MediaFile} into another file.
     *
     * @param input
     *         The {@link MediaFile} to convert.
     * @param video
     *         The {@link Codec} to use for the video.
     * @param audio
     *         The {@link Codec} to use for the audio.
     * @param subtitle
     *         The {@link Codec} to use for subtitle.
     * @param streamMapper
     *         The {@link MediaStreamMapper} to use to add {@link MediaStream} to ffmpeg.
     * @param outputDir
     *         The {@link Path} to use as output directory.
     * @param filename
     *         The filename for the output file.
     *
     * @return A {@link ConvertTask}.
     */
    public static ConvertTask<Path> of(MediaFile input, Codec video, Codec audio, Codec subtitle, MediaStreamMapper streamMapper, Path outputDir, String filename) {

        return new ConvertTask<>(
                ConvertTask::getOutputFile,
                input,
                video,
                audio,
                subtitle,
                streamMapper,
                outputDir,
                filename
        );
    }

    /**
     * Create a {@link ConvertTask} that will convert a {@link MediaFile} into multiple file for each stream.
     *
     * @param input
     *         The {@link MediaFile} to convert.
     * @param video
     *         The {@link Codec} to use for the video.
     * @param audio
     *         The {@link Codec} to use for the audio.
     * @param subtitle
     *         The {@link Codec} to use for subtitle.
     * @param streamMapper
     *         The {@link MediaStreamMapper} to use to add {@link MediaStream} to ffmpeg.
     * @param outputDir
     *         The {@link Path} to use as output directory.
     * @param streamNamer
     *         The {@link MediaStreamNamer} to use to get the output name for a {@link MediaStream}.
     *
     * @return A {@link ConvertTask}.
     */
    public static ConvertTask<Map<MediaStream, Path>> of(MediaFile input, Codec video, Codec audio, Codec subtitle, MediaStreamMapper streamMapper, Path outputDir, MediaStreamNamer streamNamer) {

        return new ConvertTask<>(
                ConvertTask::getOutputFiles,
                input,
                video,
                audio,
                subtitle,
                streamMapper,
                outputDir,
                streamNamer
        );
    }

    private final Function<ConvertTask<?>, T> resolver;

    private final MediaFile input;
    private final Codec     video;
    private final Codec             audio;
    private final Codec             subtitle;
    private final MediaStreamMapper streamMapper;
    private       Path              outputDir;

    private final String filename;

    private final Path                   outputFile;
    private final Map<MediaStream, Path> outputFiles;

    private ConvertTask(Function<ConvertTask<?>, T> resolver, MediaFile input, Codec video, Codec audio, Codec subtitle, MediaStreamMapper streamMapper, Path outputDir, String filename) {

        super(Binary.ffmpeg());
        this.resolver     = resolver;
        this.input        = input;
        this.video        = video;
        this.audio        = audio;
        this.subtitle     = subtitle;
        this.streamMapper = streamMapper;
        this.outputDir    = outputDir;
        this.filename     = filename;
        this.outputFile   = outputDir.resolve(this.filename);
        this.outputFiles  = null;
    }

    private ConvertTask(Function<ConvertTask<?>, T> resolver, MediaFile input, Codec video, Codec audio, Codec subtitle, MediaStreamMapper streamMapper, Path outputDir, MediaStreamNamer streamNamer) {

        super(Binary.ffmpeg());
        this.resolver    = resolver;
        this.input       = input;
        this.video       = video;
        this.audio       = audio;
        this.subtitle    = subtitle;
        this.outputDir   = outputDir;
        this.filename    = null;
        this.outputFile  = null;
        this.outputFiles = new HashMap<>();

        this.streamMapper = streamMapper.then((binary, stream, codec) -> {
            String filename = streamNamer.name(stream, codec);

            Path output = this.outputDir.resolve(filename).normalize();
            if (!output.startsWith(this.outputDir)) {
                throw new IllegalArgumentException("Illegal filename: " + filename);
            }

            binary.addArgument(output.getFileName().toString());
            this.outputFiles.put(stream, output);
        });
    }

    private Path getOutputFile() {

        return this.outputFile;
    }

    private Map<MediaStream, Path> getOutputFiles() {

        return this.outputFiles;
    }

    /**
     * Map all the stream of the input files to its corresponding codec.
     *
     * @param ffmpeg
     *         The {@link Binary} into which the arguments will be appended.
     */
    public void mapStreams(Binary ffmpeg) {

        for (MediaStream stream : this.input.getStreams()) {
            switch (stream.getCodec().getType()) {
                case VIDEO -> {
                    if (this.video == null) continue;
                    this.streamMapper.map(ffmpeg, stream, this.video);
                }
                case AUDIO -> {
                    if (this.audio == null) continue;
                    this.streamMapper.map(ffmpeg, stream, this.audio);
                }
                case SUBTITLE -> {
                    if (this.subtitle == null) continue;
                    this.streamMapper.map(ffmpeg, stream, this.subtitle);
                }
            }
        }
    }

    @Override
    public void preprocess(Binary ffmpeg) throws IOException {

        if (this.outputDir == null) {
            this.outputDir = Path.of(".").toAbsolutePath().normalize();
        }

        ffmpeg.setBaseDir(this.outputDir);
        ffmpeg.addArguments("-i", this.input.getPath().toString());

        if (this.filename != null) {
            this.preprocessSingleFile(ffmpeg);
        } else {
            this.preprocessMultipleFile(ffmpeg);
        }
    }

    private void preprocessSingleFile(Binary ffmpeg) {

        Path outputFile = this.outputDir.resolve(this.filename);

        if (!outputFile.startsWith(this.outputDir)) {
            throw new IllegalArgumentException("Invalid filename: " + this.filename);
        }

        this.mapStreams(ffmpeg);
        ffmpeg.addArgument(outputFile.toString());
    }

    private void preprocessMultipleFile(Binary ffmpeg) throws IOException {

        this.mapStreams(ffmpeg);

        // Ensure outputs do not exist or ffmpeg is going to make a whim
        for (Path outputFile : this.outputFiles.values()) {
            if (Files.isRegularFile(outputFile)) {
                Files.delete(outputFile);
                continue;
            }

            if (Files.exists(outputFile)) {
                throw new IllegalStateException("Could not delete " + outputFile);
            }
        }
    }

    @Override
    public T postprocess(int code) throws IOException {

        return this.resolver.apply(this);
    }

}
