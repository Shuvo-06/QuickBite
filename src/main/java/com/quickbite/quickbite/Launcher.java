package com.quickbite.quickbite;

import javafx.application.Application;

/**
 * Entry point of QuickBite.
 * It only starts the JavaFX runtime and hands control to QuickBiteApp.
 * A plain main() class (rather than main() living inside Application) avoids
 * "JavaFX runtime components are missing" errors on some setups.
 */
public class Launcher {

    public static void main(String[] args) {
        Application.launch(QuickBiteApp.class, args);
    }
}
