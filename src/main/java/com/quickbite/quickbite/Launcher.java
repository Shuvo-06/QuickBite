package com.quickbite.quickbite;

import javafx.application.Application;

/**
 * Entry point of QuickBite.
 * It only starts the JavaFX runtime and hands control to QuickBiteApp.
 */
public class Launcher {

    public static void main(String[] args) {
        Application.launch(QuickBiteApp.class, args);
    }
}