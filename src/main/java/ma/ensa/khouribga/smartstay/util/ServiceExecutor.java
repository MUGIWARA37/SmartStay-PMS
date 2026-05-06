package ma.ensa.khouribga.smartstay.util;

import javafx.application.Platform;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ServiceExecutor {

    private static final ExecutorService executor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() + 1,
        r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    );

    /**
     * Executes a task in the background and processes the result on the UI thread.
     */
    public static <T> void execute(Supplier<T> backgroundTask, Consumer<T> uiCallback, Consumer<Throwable> errorCallback) {
        executor.submit(() -> {
            try {
                T result = backgroundTask.get();
                if (uiCallback != null) {
                    Platform.runLater(() -> uiCallback.accept(result));
                }
            } catch (Throwable t) {
                if (errorCallback != null) {
                    Platform.runLater(() -> errorCallback.accept(t));
                } else {
                    Platform.runLater(() -> AlertUtil.showError("Application Error", t.getMessage()));
                }
            }
        });
    }

    /**
     * Executes a task in the background with no return value.
     */
    public static void execute(Runnable backgroundTask, Runnable uiCallback, Consumer<Throwable> errorCallback) {
        executor.submit(() -> {
            try {
                backgroundTask.run();
                if (uiCallback != null) {
                    Platform.runLater(uiCallback);
                }
            } catch (Throwable t) {
                if (errorCallback != null) {
                    Platform.runLater(() -> errorCallback.accept(t));
                } else {
                    Platform.runLater(() -> AlertUtil.showError("Application Error", t.getMessage()));
                }
            }
        });
    }

    /**
     * Submits a Runnable task for execution in the background.
     */
    public static void submit(Runnable task) {
        executor.submit(task);
    }

    public static void shutdown() {
        executor.shutdown();
    }
}
