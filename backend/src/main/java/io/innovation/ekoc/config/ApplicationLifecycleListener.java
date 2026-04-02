package io.innovation.ekoc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener for application lifecycle events that logs important state transitions.
 */
@Slf4j
@Component
public class ApplicationLifecycleListener {

    /**
     * Called when the application is starting up (very early in the startup process).
     */
    @EventListener(ApplicationStartingEvent.class)
    public void onApplicationStarting() {
        log.info("Application starting - initialization phase");
    }

    /**
     * Called when the application has started and is ready to process requests.
     */
    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        log.info("Application started successfully - context initialized");
    }

    /**
     * Called when the application is fully ready to serve traffic.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application readiness achieved - ready to accept requests");
    }

    /**
     * Called when the application context is being closed (shutdown initiated).
     */
    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        log.info("Shutdown initiated - closing application context");
    }

    /**
     * Shutdown hook to log when the application shutdown is complete.
     */
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown complete - application terminated");
        }, "shutdown-hook"));
    }
}
