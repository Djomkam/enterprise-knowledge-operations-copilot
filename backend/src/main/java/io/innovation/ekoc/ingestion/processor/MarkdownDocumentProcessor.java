package io.innovation.ekoc.ingestion.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MarkdownDocumentProcessor implements DocumentProcessor {

    private static final Pattern MARKDOWN_SYNTAX = Pattern.compile(
            "(?m)^#{1,6}\\s+|" +        // headings
            "\\*{1,3}([^*]+)\\*{1,3}|" + // bold/italic
            "_{1,3}([^_]+)_{1,3}|" +      // bold/italic underscores
            "`{1,3}[^`]*`{1,3}|" +        // inline code / code blocks
            "!?\\[([^]]*)]\\([^)]*\\)|"   + // links and images
            "^\\s*[-*+]\\s+|" +            // unordered list markers
            "^\\s*\\d+\\.\\s+"             // ordered list markers
    );

    @Override
    public String extract(InputStream inputStream) throws IOException {
        log.debug("Extracting text from Markdown file");
        String raw = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        String text = MARKDOWN_SYNTAX.matcher(raw).replaceAll("$1$2$3").strip();
        log.debug("Extracted {} characters from Markdown file", text.length());
        return text;
    }

    @Override
    public boolean supports(String contentType) {
        return "text/markdown".equalsIgnoreCase(contentType)
                || "text/x-markdown".equalsIgnoreCase(contentType);
    }
}
