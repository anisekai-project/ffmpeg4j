package fr.anisekai.media.bin.wrapper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Interface representing a ffmpeg task.
 *
 * @param <T>
 *         Type of the result for the {@link FFMpegCommand}.
 */
public interface FFMpegCommand<T> {

    /**
     * Define the amount of time to wait before considering this ffmpeg task as zombie and killing the process.
     *
     * @param timeout
     *         The maximum time to wait
     * @param unit
     *         The time unit of the {@code timeout} argument
     *
     * @return The same instance, for chaining
     */
    FFMpegCommand<T> timeout(long timeout, TimeUnit unit);

    /**
     * Run the ffmpeg task and return its result
     *
     * @return The result of the specific task being executed
     *
     * @throws IOException
     *         If an I/O error occurs while running ffmpeg
     * @throws InterruptedException
     *         If something happened while waiting for ffmpeg to end.
     * @throws IllegalStateException
     *         If the timeout defined has been reached without ffmpeg ending its task.
     */
    T run() throws IOException, InterruptedException;

}
