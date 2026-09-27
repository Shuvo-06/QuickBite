package com.quickbite.quickbite.service;

import javafx.application.Platform;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Periodically checks internet connectivity on a background thread and reports changes back on
 * the JavaFX Application Thread. QuickBite uses this to show a "you are offline" banner and to
 * block placing an order while the connection is down, per the assignment's requirement that
 * ordering must fail gracefully ("cannot connect to server") when there is no network.
 *
 * The check itself is a quick raw socket connection attempt (with a short timeout) rather than
 * a full HTTP request, since all we need to know is "is the network reachable right now?".
 */
public class NetworkMonitor implements Shutdownable {

    private static final String PROBE_HOST = "8.8.8.8"; // a well-known, highly available address
    private static final int PROBE_PORT = 53;            // DNS port: almost never blocked by firewalls
    private static final int PROBE_TIMEOUT_MS = 2000;
    private static final int CHECK_INTERVAL_SECONDS = 4;

    private static final NetworkMonitor INSTANCE = new NetworkMonitor();

    // volatile: written by the background checker thread, read by the JavaFX thread.
    private volatile boolean online = true;

    private final List<Consumer<Boolean>> listeners = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "network-monitor");
        thread.setDaemon(true);
        return thread;
    });

    private NetworkMonitor() {
        // Runs forever on its own background thread; never touches the JavaFX thread directly.
        scheduler.scheduleWithFixedDelay(this::checkConnection, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public static NetworkMonitor getInstance() {
        return INSTANCE;
    }

    /** Safe to call from any thread; reflects the most recent background check. */
    public boolean isOnline() {
        return online;
    }

    /** Called immediately with the current state, then again every time the state changes. */
    public void addListener(Consumer<Boolean> listener) {
        listeners.add(listener);
        Platform.runLater(() -> listener.accept(online));
    }

    public void removeListener(Consumer<Boolean> listener) {
        listeners.remove(listener);
    }

    @Override
    public void shutdown() {
        scheduler.shutdownNow();
    }

    /** Runs on the "network-monitor" background thread. Never called from the JavaFX thread. */
    private void checkConnection() {
        boolean reachable;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(PROBE_HOST, PROBE_PORT), PROBE_TIMEOUT_MS);
            reachable = true;
        } catch (IOException e) {
            reachable = false;
        }

        if (reachable != online) {
            online = reachable;
            boolean finalReachable = reachable;
            Platform.runLater(() -> {
                for (Consumer<Boolean> listener : listeners) {
                    listener.accept(finalReachable);
                }
            });
        }
    }
}
