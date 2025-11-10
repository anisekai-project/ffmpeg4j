package fr.anisekai.media.bin.wrapper.tasks;

import fr.anisekai.media.bin.Binary;
import fr.anisekai.media.bin.wrapper.FFMpegCommandTask;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represent a ffmpeg task used to probe a file to gets its information.
 */
public class ProbeTask extends FFMpegCommandTask<JSONObject> {

    private final Path input;
    private final Path output;

    /**
     * Create a new {@link ProbeTask}
     *
     * @param input
     *         The {@link Path} pointing to the file to read
     * @param output
     *         The {@link Path} pointing to the file where the result will be written temporarily.
     */
    public ProbeTask(Path input, Path output) {

        super(Binary.ffprobe());
        this.input  = input.toAbsolutePath().normalize();
        this.output = output.toAbsolutePath().normalize();
    }

    @Override
    public void preprocess(Binary ffmpeg) {

        if (!Files.isRegularFile(this.input)) {
            throw new IllegalArgumentException("Input file does not exists.");
        }

        ffmpeg.addArgument("-show_streams");
        ffmpeg.addArguments("-of", "json");
        ffmpeg.addArguments("-i", this.input.toString());
        ffmpeg.addArguments("-o", this.output.toString());
    }

    @Override
    public JSONObject postprocess(int code) throws IOException {

        JSONObject json = new JSONObject(Files.readString(this.output, StandardCharsets.UTF_8));
        Files.delete(this.output);
        return json;
    }

}
