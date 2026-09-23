package com.quickbite.quickbite.service;

import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderStatus;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Moves every placed order through its statuses automatically, in the background.
 *
 * Threads involved:
 *   - JavaFX Application Thread : the UI. Never blocked by this class.
 *   - order-tracker-N threads   : a ScheduledExecutorService pool that changes the order status
 *
 * Flow for each step:
 *   background thread: wait a random time -> change the status
 *        -> Platform.runLater(...) -> UI thread tells all listeners (pop-up, delivery window)
 */
public class OrderTrackingService {

    // Random delay between two status changes (short, so it suits a classroom demo)
    private static final int MIN_DELAY_MS = 3000;
    private static final int MAX_DELAY_MS = 8000;

    // One shared tracker for the whole application
    private static final OrderTrackingService INSTANCE = new OrderTrackingService();

    private final OrderService orderService = new OrderService();

    // CopyOnWriteArrayList is safe when a listener is added or removed while we loop over the list.
    private final List<Consumer<Order>> listeners = new CopyOnWriteArrayList<>();

    private final AtomicInteger threadCounter = new AtomicInteger(1);

    // A pool of 2 background threads. Daemon threads never keep the JVM alive after the window closes.
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "order-tracker-" + threadCounter.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    });

    private OrderTrackingService() {
    }

    public static OrderTrackingService getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------
    // Listeners (always called on the JavaFX Application Thread)
    // ------------------------------------------------------------------

    public void addListener(Consumer<Order> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<Order> listener) {
        listeners.remove(listener);
    }

    // ------------------------------------------------------------------
    // Tracking
    // ------------------------------------------------------------------

    /** Starts the automatic progression of a newly placed order. */
    public void track(Order order) {
        scheduleNextStep(order);
    }

    /** Stops all background work. Called from QuickBiteApp.stop() when the application closes. */
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void scheduleNextStep(Order order) {
        int delay = ThreadLocalRandom.current().nextInt(MIN_DELAY_MS, MAX_DELAY_MS + 1);
        try {
            // schedule() returns immediately. The scheduler runs the task later, so no thread sleeps.
            scheduler.schedule(() -> runStep(order), delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            // The application is closing and the scheduler is already shut down: nothing to do.
        }
    }

    /** Runs on a BACKGROUND thread (order-tracker-N), never on the UI thread. */
    private void runStep(Order order) {
        try {
            // In the SQLite phase, the database UPDATE goes here: it belongs on the background thread.
            orderService.advanceStatus(order);
            OrderStatus newStatus = order.getStatus();

            System.out.println("[" + Thread.currentThread().getName() + "] Order #"
                    + order.getId() + " -> " + newStatus);

            // Only the JavaFX Application Thread may touch the UI, so hand the notification over.
            Platform.runLater(() -> {
                for (Consumer<Order> listener : listeners) {
                    listener.accept(order);
                }
            });

            if (!newStatus.isFinished()) {
                scheduleNextStep(order);
            }
        } catch (Exception e) {
            // A scheduled task that throws would fail silently and the order would freeze,
            // so we log the problem ourselves.
            e.printStackTrace();
        }
    }
}