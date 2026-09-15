package fr.anisekai.media.bin;

import fr.anisekai.media.bin.wrapper.FFMpegCommand;
import fr.anisekai.media.bin.wrapper.FFMpegCommandTask;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@DisplayName("Binary (cancellable execution)")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class BinaryTests {

    private static Binary sleep(long seconds) {

        Binary binary = new Binary("sleep");
        binary.addArgument(seconds);
        return binary;
    }

    private static void cancelAfter(AtomicBoolean cancelled, long delay, TimeUnit unit) {

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(unit.toMillis(delay));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cancelled.set(true);
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean hasSleepChild() {

        return ProcessHandle.current()
                            .children()
                            .map(ProcessHandle::info)
                            .anyMatch(info -> info.command()
                                                   .map(command -> command.endsWith("/sleep") || command.equals("sleep"))
                                                   .orElse(false));
    }

    private static void assertNoSleepChild() throws InterruptedException {

        Instant deadline = Instant.now().plusSeconds(5);
        while (hasSleepChild() && Instant.now().isBefore(deadline)) {
            Thread.sleep(100);
        }
        Assertions.assertFalse(hasSleepChild(), "Cancelled sleep process is still running");
    }

    @Test
    @DisplayName("execute | Completes without cancellation")
    public void testExecuteWithoutCancel() {

        int code = Assertions.assertDoesNotThrow(() -> sleep(0).execute(1, TimeUnit.MINUTES));
        Assertions.assertEquals(0, code, "Exit code mismatch");
    }

    @Test
    @DisplayName("execute | Cancellation destroys the process")
    public void testExecuteCancelled() {

        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancelAfter(cancelled, 500, TimeUnit.MILLISECONDS);

        Instant start = Instant.now();
        InterruptedException ex = Assertions.assertThrows(
                InterruptedException.class,
                () -> sleep(30).execute(1, TimeUnit.MINUTES, cancelled::get)
        );
        Duration elapsed = Duration.between(start, Instant.now());

        Assertions.assertEquals("Execution cancelled", ex.getMessage(), "Exception message mismatch");
        Assertions.assertTrue(elapsed.compareTo(Duration.ofSeconds(15)) < 0,
                "Execution was not aborted promptly, took " + elapsed);
        Assertions.assertDoesNotThrow(BinaryTests::assertNoSleepChild);
    }

    @Test
    @DisplayName("execute | Timeout behavior is unchanged")
    public void testExecuteTimeout() {

        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> sleep(30).execute(1, TimeUnit.SECONDS, () -> false)
        );
        Assertions.assertEquals("Process timed out", ex.getMessage(), "Exception message mismatch");
    }

    @Test
    @DisplayName("command | Cancellation is plumbed through run")
    public void testCommandCancellable() {

        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancelAfter(cancelled, 500, TimeUnit.MILLISECONDS);

        FFMpegCommandTask<Integer> command = new FFMpegCommandTask<>(sleep(30)) {
            @Override
            public void preprocess(Binary ffmpeg) throws IOException {
            }

            @Override
            public Integer postprocess(int code) throws IOException {

                return code;
            }
        };

        Assertions.assertSame(command, command.cancellable(cancelled::get), "Chaining instance mismatch");
        command.timeout(1, TimeUnit.MINUTES);

        Instant start = Instant.now();
        Assertions.assertThrows(InterruptedException.class, command::run);
        Duration elapsed = Duration.between(start, Instant.now());

        Assertions.assertTrue(elapsed.compareTo(Duration.ofSeconds(15)) < 0,
                "Command was not aborted promptly, took " + elapsed);
        Assertions.assertDoesNotThrow(BinaryTests::assertNoSleepChild);
    }

    @Test
    @DisplayName("command | Default ignores the signal")
    public void testCommandDefaultCancellable() {

        FFMpegCommand<Integer> command = new FFMpegCommand<>() {
            @Override
            public FFMpegCommand<Integer> timeout(long timeout, TimeUnit unit) {

                return this;
            }

            @Override
            public Integer run() {

                return 0;
            }
        };

        Assertions.assertSame(command, command.cancellable(() -> true), "Default must return the same instance");
        Assertions.assertEquals(0, Assertions.assertDoesNotThrow(command::run), "Default must ignore the signal");
    }
}
