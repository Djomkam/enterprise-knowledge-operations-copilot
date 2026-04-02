package io.innovation.ekoc.ai.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
public class OllamaProvider implements AIProvider {

    @Override
    public String getProviderName() {
        return "ollama";
    }
}
