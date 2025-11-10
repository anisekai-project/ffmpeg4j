package fr.anisekai.media.bin.wrapper.commands;

import fr.anisekai.media.MediaMeta;
import fr.anisekai.media.bin.wrapper.FFMpegCommand;
import fr.anisekai.media.bin.wrapper.tasks.CombineTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link FFMpegCommand} builder specifically built to handle combining {@link MediaMeta} into a single file.
 */
public class CombineCommandBuilder {

    private final List<MediaMeta> metadata = new ArrayList<>();

    /**
     * Create a new instance of {@link CombineCommandBuilder}
     *
     * @param input
     *         The first {@link MediaMeta} to combine.
     */
    public CombineCommandBuilder(MediaMeta input) {

        this.with(input);
    }

    /**
     * Add a {@link MediaMeta} into this {@link CombineCommandBuilder}.
     *
     * @param meta
     *         A {@link MediaMeta}.
     *
     * @return The same instance for chaining
     */
    public CombineCommandBuilder with(MediaMeta meta) {

        this.metadata.add(meta);
        return this;
    }

    /**
     * Set the {@link Path} pointing to the file into which all {@link MediaMeta} will be combined.
     *
     * @param file
     *         The {@link Path} pointing to a file.
     *
     * @return A {@link FFMpegCommand} ready to execute this {@link CombineCommandBuilder}.
     */
    public FFMpegCommand<Path> file(Path file) {

        Path normalized = file.toAbsolutePath().normalize();

        if (Files.exists(normalized)) {
            throw new IllegalArgumentException(String.format("The path '%s' already exists", normalized));
        }

        return new CombineTask(this.metadata, normalized);
    }

}
