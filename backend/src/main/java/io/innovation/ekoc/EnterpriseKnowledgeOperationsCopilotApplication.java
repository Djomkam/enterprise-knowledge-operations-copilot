package io.innovation.ekoc;

import io.innovation.ekoc.config.ApplicationLifecycleListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class EnterpriseKnowledgeOperationsCopilotApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(
                EnterpriseKnowledgeOperationsCopilotApplication.class, args);
        
        // Register shutdown hook for logging
        ApplicationLifecycleListener listener = context.getBean(ApplicationLifecycleListener.class);
        listener.registerShutdownHook();
    }

}
