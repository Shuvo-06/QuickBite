package com.quickbite.quickbite.service;

/**
 * Anything that owns a background thread pool and must release it when the app closes.
 * OrderTrackingService, ApiService, and NetworkMonitor all implement this so QuickBiteApp.stop()
 * can shut every one of them down the same way, through one interface, without needing to know
 * how each service works internally — programming to an interface rather than a concrete class.
 */
public interface Shutdownable {
    void shutdown();
}