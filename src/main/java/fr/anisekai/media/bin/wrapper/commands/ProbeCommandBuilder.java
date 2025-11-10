package fr.anisekai.media.bin.wrapper.commands;

import fr.anisekai.media.MediaFile;
import fr.anisekai.media.bin.wrapper.FFMpegCommand;
import fr.anisekai.media.bin.wrapper.tasks.ProbeTask;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link FFMpegCommand} builder specifically built to analyze a {@link Path} content using ffprobe.
 */
public class ProbeCommandBuilder {

    private final Path input;

    /**
     * Create a new instance of {@link ProbeCommandBuilder}.
     *
     * @param input
     *         The {@link Path} pointing to the file to analyze.
     */
    public ProbeCommandBuilder(Path input) {

        this.input = input.toAbsolutePath().normalize();
    }

    /**
     * Create a temporary file into which the JSON data will be written before being read and deleted.
     *
     * @return A {@link FFMpegCommand} ready to analyze the {@link MediaFile}.
     *
     * @throws IOException
     *         If the temporary file could not be created.
     */
    public FFMpegCommand<JSONObject> intoTemporary() throws IOException {

        return new ProbeTask(this.input, Files.createTempFile("ansk-", ".json"));
    }

    /**
     * Set the {@link Path} pointing to the file into which the JSON data will be written before being read and
     * deleted.
     *
     * @param output
     *         The {@link Path} pointing to a file.
     *
     * @return A {@link FFMpegCommand} ready to analyze the {@link MediaFile}.
     */
    public FFMpegCommand<JSONObject> into(Path output) {

        Path normalized = output.toAbsolutePath().normalize();

        if (Files.exists(normalized)) {
            throw new IllegalArgumentException(String.format("The path '%s' already exists", normalized));
        }

        return new ProbeTask(this.input, normalized);
    }

}
