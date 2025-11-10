package fr.anisekai.media.bin.wrapper.tasks;

import fr.anisekai.media.MediaFile;
import fr.anisekai.media.MediaStream;
import fr.anisekai.media.bin.Binary;
import fr.anisekai.media.bin.wrapper.FFMpegCommand;
import fr.anisekai.media.bin.wrapper.FFMpegCommandTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specific implementation {@link FFMpegCommand} allowing to convert a {@link MediaFile} into chunks with a DASH meta
 * file.
 */
public class MpdTask extends FFMpegCommandTask<Path> {

    /**
     * Recursively deletes the provided {@link Path}. If it's a directory, its content will be deleted first.
     *
     * @param path
     *         The {@link Path} of the directory or file to delete.
     *
     * @throws IOException
     *         If any deletion fails
     */
    private static void delete(Path path) throws IOException {

        if (!Files.exists(path)) {
            return;
        }

        if (Files.isRegularFile(path)) {
            Files.delete(path);
            return;
        }

        if (Files.isDirectory(path)) {
            Files.walkFileTree(
                    path,
                    new SimpleFileVisitor<>() {

                        @Override
                        public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {

                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, @Nullable IOException exc) throws IOException {

                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    }
            );
            return;
        }

        throw new UnsupportedOperationException("Unable to delete path: " + path);
    }

    /**
     * Make sure the provided {@link Path} is a directory.
     *
     * @param path
     *         The {@link Path}
     *
     * @throws IOException
     *         If the directory could not be created.
     */
    private static void ensureDirectory(Path path) throws IOException {

        if (Files.isDirectory(path)) {
            return;
        }

        if (Files.isRegularFile(path)) {
            throw new IllegalStateException(path + " is a regular file");
        }

        if (!Files.exists(path)) {
            Files.createDirectories(path);
            return;
        }

        throw new IllegalStateException(path + " exists and it is neither a directory or regular file");
    }

    private final MediaFile         input;
    private final Path              output;
    private final Path              mdp;
    private final List<MediaStream> adaptionSetsMedia;

    /**
     * Create a new {@link MpdTask} instance.
     *
     * @param input
     *         The {@link MediaFile} to convert.
     * @param output
     *         The {@link Path} to use as output directory
     * @param mdp
     *         The {@link Path} to use for the meta file.
     */
    public MpdTask(MediaFile input, Path output, Path mdp) {

        super(Binary.ffmpeg());
        this.input             = input;
        this.output            = output;
        this.mdp               = mdp;
        this.adaptionSetsMedia = new ArrayList<>();
    }

    @Override
    public void preprocess(Binary ffmpeg) throws IOException {

        ffmpeg.setBaseDir(this.output);
        ffmpeg.addArguments("-i", this.input.getPath().toString());

        Collection<String> adaptationSets = new ArrayList<>();

        int id = 0;
        for (MediaStream stream : this.input.getStreams()) {
            switch (stream.getCodec().getType()) {
                case VIDEO, AUDIO:
                    ffmpeg.addArguments("-map", "0:%s".formatted(stream.getId()));
                    adaptationSets.add("id=%s,streams=%s".formatted(id, id));
                    this.adaptionSetsMedia.add(stream);
                    id++;
                    break;
            }
        }

        ffmpeg.addArguments("-c", "copy");
        ffmpeg.addArguments("-adaptation_sets", String.join(" ", adaptationSets));
        ffmpeg.addArguments(this.mdp.toString());

        // Ensure output is empty or ffmpeg is going to make a whim
        delete(this.output);
        ensureDirectory(this.output);
    }

    @Override
    public Path postprocess(int code) throws IOException {

        Pattern pattern = Pattern.compile("<AdaptationSet id=\"(?<idx>\\d+)\"");

        Path temp = Files.createTempFile("patched-", ".mpd");

        try (
                BufferedReader reader = Files.newBufferedReader(this.mdp);
                BufferedWriter writer = Files.newBufferedWriter(temp, StandardOpenOption.TRUNCATE_EXISTING)
        ) {
            String line;

            //noinspection NestedAssignment
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);

                if (matcher.find()) {
                    int idx = Integer.parseInt(matcher.group("idx"));

                    if (idx >= 0 && idx < this.adaptionSetsMedia.size()) {
                        MediaStream stream = this.adaptionSetsMedia.get(idx);

                        if (stream.getMetadata().containsKey("title")) {
                            String label = stream.getMetadata().get("title")
                                                 .replace("&", "&amp;")
                                                 .replace("\"", "&quot;")
                                                 .replace("<", "&lt;")
                                                 .replace(">", "&gt;");

                            line = line.replaceFirst(
                                    "id=\"%d\"".formatted(idx),
                                    "id=\"%d\" label=\"%s\"".formatted(idx, label)
                            );
                        }
                    }
                }

                writer.write(line);
                writer.newLine();
            }
        }

        // Atomically replace original MPD
        Files.move(temp, this.mdp, StandardCopyOption.REPLACE_EXISTING);

        return this.mdp;
    }

}
