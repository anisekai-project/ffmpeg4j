package fr.anisekai.media.bin.wrapper.commands;

import fr.anisekai.media.MediaFile;
import fr.anisekai.media.bin.wrapper.FFMpegCommand;
import fr.anisekai.media.bin.wrapper.tasks.MpdTask;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link FFMpegCommand} builder specifically built to convert a {@link MediaFile} streams into chunks with a DASH xml
 * meta file.
 */
public class MpdCommandBuilder {

    private final MediaFile input;
    private       Path      outputDirectory;

    /**
     * Create a new {@link MpdCommandBuilder}.
     *
     * @param input
     *         The {@link MediaFile} for which chunks will be generated.
     */
    public MpdCommandBuilder(MediaFile input) {

        this.input = input;
    }

    /**
     * Set the {@link Path} pointing to the directory into which the chunks will be generated.
     *
     * @param outputDirectory
     *         The {@link Path} pointing directory.
     *
     * @return The same instance for chaining.
     */
    public MpdCommandBuilder into(Path outputDirectory) {

        Path normalized = outputDirectory.toAbsolutePath().normalize();

        if (!Files.isDirectory(normalized)) {
            throw new IllegalArgumentException(String.format("The path '%s' is not a directory", normalized));
        }

        this.outputDirectory = normalized;
        return this;
    }

    /**
     * Set the filename into which the meta file will be written. If {@link #into(Path)} was not called before, the
     * current runtime working directory will be used as output directory. All chunks will be written alongside the
     * metafile.
     *
     * @param filename
     *         The name of the metafile. Must have the extension 'mpd'.
     *
     * @return A {@link FFMpegCommand} ready to convert the {@link MediaFile} into DASH chunks.
     */
    public FFMpegCommand<Path> as(String filename) {

        if (!filename.endsWith(".mpd")) {
            throw new IllegalArgumentException(String.format(
                    "The file name '%s' does not have the .mpd extension",
                    filename
            ));
        }

        if (this.outputDirectory == null) {
            this.outputDirectory = Path.of(".").toAbsolutePath().normalize();
        }

        Path file = this.outputDirectory.resolve(filename);

        if (!file.startsWith(this.outputDirectory)) {
            throw new IllegalArgumentException(String.format("The file name '%s' is invalid.", filename));
        }

        return new MpdTask(this.input, this.outputDirectory, file);
    }

}
