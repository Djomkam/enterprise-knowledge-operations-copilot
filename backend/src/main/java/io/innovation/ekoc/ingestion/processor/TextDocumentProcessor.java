package io.innovation.ekoc.ingestion.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class TextDocumentProcessor implements DocumentProcessor {

    @Override
    public String extract(InputStream inputStream) throws IOException {
        log.debug("Extracting text from plain text file");
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        log.debug("Extracted {} characters from text file", text.length());
        return text;
    }

    @Override
    public boolean supports(String contentType) {
        return "text/plain".equalsIgnoreCase(contentType);
    }
}
