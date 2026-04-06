package io.innovation.ekoc.ingestion.processor;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentProcessor {

    String extract(InputStream inputStream) throws IOException;

    boolean supports(String contentType);
}
