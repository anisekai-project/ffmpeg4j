package fr.anisekai.media.interfaces;

/**
 * Listener receiving machine-friendly conversion progress reports while an ffmpeg task runs.
 * <p>
 * Callbacks fire on a dedicated monitor thread, not on the thread running the conversion:
 * implementations must be thread-safe.
 */
@FunctionalInterface
public interface ProgressListener {

    /**
     * Called when ffmpeg reports a new progress block.
     *
     * @param report
     *         The parsed progress report.
     */
    void onProgress(ProgressReport report);
}
