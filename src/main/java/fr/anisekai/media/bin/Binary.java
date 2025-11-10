package fr.anisekai.media.bin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class allowing a better handling of program arguments.
 */
public class Binary {

    /**
     * Create a new {@link Binary} instance for the ffprobe program.
     *
     * @return A {@link Binary} instance.
     */
    public static Binary ffprobe() {

        return new Binary("ffprobe");
    }

    /**
     * Create a new {@link Binary} instance for the ffmpeg program.
     *
     * @return A {@link Binary} instance.
     */
    public static Binary ffmpeg() {

        return new Binary("ffmpeg");
    }

    private final List<String> args;
    private final List<String> holdArgs;
    private       Path         baseDir = null;

    /**
     * Create a new {@link Binary} instance
     *
     * @param executable
     *         Program executable path/name
     */
    public Binary(String executable) {

        this.args     = new ArrayList<>();
        this.holdArgs = new ArrayList<>();
        this.addArgument(executable);
    }

    /**
     * Set the base directory (working directory) for this {@link Binary} execution.
     *
     * @param baseDir
     *         The {@link Path} pointing to the base directory.
     */
    public void setBaseDir(Path baseDir) {

        this.baseDir = baseDir;
    }

    /**
     * Add an execution command line argument.
     *
     * @param argument
     *         An argument
     */
    public void addArgument(Object argument) {

        this.args.add(argument.toString());
    }

    /**
     * Add multiple execution command line arguments.
     *
     * @param args
     *         An array of arguments
     */
    public void addArguments(Object... args) {

        Arrays.stream(args).map(Object::toString).forEach(this.args::add);
    }

    /**
     * Add a "hold" execution command line argument. Those arguments will not be immediately appended to the command but
     * instead will be held until {@link #commitHoldArguments} is called.
     *
     * @param argument
     *         An argument
     */
    public void addHoldArgument(Object argument) {

        this.holdArgs.add(argument.toString());
    }

    /**
     * Add multiple "hold" execution command line arguments. Those arguments will not be immediately appended to the
     * command but instead will be held until {@link #commitHoldArguments} is called.
     *
     * @param args
     *         An array of arguments
     */
    public void addHoldArguments(Object... args) {

        Arrays.stream(args).map(Object::toString).forEach(this.holdArgs::add);
    }

    /**
     * Append all "hold" execution command line arguments to the effective list of arguments.
     */
    public void commitHoldArguments() {

        this.args.addAll(this.holdArgs);
        this.holdArgs.clear();
    }

    /**
     * Execute this {@link Binary} and wait for the provided timeout. If the timeout is reached, the execution will be
     * canceled and {@link InterruptedException} will be thrown.
     *
     * @param timeout
     *         The amount of unit to wait for the execution to finish.
     * @param unit
     *         The unit of scale for the timeout.
     *
     * @return The exit code of the executed program.
     *
     * @throws IOException
     *         Thrown if the program could not be executed.
     * @throws InterruptedException
     *         Thrown if the program did not finish its execution before the timeout as been reached.
     */
    public int execute(long timeout, TimeUnit unit) throws IOException, InterruptedException {

        // Commit hold args
        this.commitHoldArguments();

        ProcessBuilder builder = new ProcessBuilder(this.args);

        if (this.baseDir != null) {
            builder.directory(this.baseDir.toFile());
        }

        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process process = builder.start();

        process.getOutputStream().close();

        boolean exitedNormally = process.waitFor(timeout, unit);

        if (!exitedNormally) {
            process.destroyForcibly();
            throw new IllegalStateException("Process timed out");
        }
        return process.exitValue();
    }

}
