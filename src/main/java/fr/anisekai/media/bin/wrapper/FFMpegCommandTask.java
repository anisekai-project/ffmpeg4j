package fr.anisekai.media.bin.wrapper;

import fr.anisekai.media.bin.Binary;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Abstract implementation of {@link FFMpegCommand} that handle process execution and timeout.
 *
 * @param <T>
 *         Type of the result for the {@link FFMpegCommandTask}.
 */
public abstract class FFMpegCommandTask<T> implements FFMpegCommand<T> {

    private final Binary binary;

    private long     timeout = 1;
    private TimeUnit unit    = TimeUnit.MINUTES;

    /**
     * Create this {@link FFMpegCommandTask} with a specific {@link Binary} instance.
     *
     * @param binary
     *         The {@link Binary} to use for this task.
     */
    public FFMpegCommandTask(Binary binary) {

        this.binary = binary;
    }

    @Override
    public FFMpegCommand<T> timeout(long timeout, TimeUnit unit) {

        this.timeout = timeout;
        this.unit    = unit;
        return this;
    }

    /**
     * Method called before the execution of ffmpeg, allowing tasks to implement their own logic for the process
     * arguments.
     *
     * @param ffmpeg
     *         The {@link Binary} instance in use.
     *
     * @throws IOException
     *         If something happen during file preparation
     */
    public abstract void preprocess(Binary ffmpeg) throws IOException;

    /**
     * Method called after the execution of ffmpeg, allowing tasks to implement their own logic for task cleanup and
     * returning the expected result.
     *
     * @param code
     *         The ffmpeg exit code.
     *
     * @return The task result.
     *
     * @throws IOException
     *         If something happen while handling the result
     */
    public abstract T postprocess(int code) throws IOException;

    @Override
    public T run() throws IOException, InterruptedException {

        this.preprocess(this.binary);
        int code = this.binary.execute(this.timeout, this.unit);
        return this.postprocess(code);
    }

}
