package com.examagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Stage 10: talks to the Python OCR sidecar (ocr-sidecar/main.py) over plain HTTP. A
 * separate process rather than an in-JVM call because the OCR engine itself
 * (pytesseract/Tesseract) is Python-only - this client is the entire integration surface,
 * so if the sidecar were ever swapped for a different engine or rewritten, nothing else in
 * the Java app would need to change.
 *
 * <p>Two real bugs had to be found and fixed to get this working (see DEVLOG Stage 10):
 * (1) the multipart body is built by hand rather than via {@code MultiValueMap} +
 * {@code FormHttpMessageConverter}, after three attempts at that "standard" approach all
 * produced a request the sidecar's FastAPI parser saw as having no "file" part at all; and
 * (2) the underlying JDK {@link HttpClient} defaults to attempting an HTTP/2 cleartext
 * ("h2c") upgrade, which uvicorn does not handle the way the JDK client expects - the
 * upgrade attempt silently produced a request body FastAPI never correctly received, even
 * though the raw bytes were valid (confirmed by capturing them against a throwaway
 * {@code http.server} that doesn't care about the Connection/Upgrade headers). Forcing
 * HTTP/1.1 fixed it.
 */
@Component
public class OcrClient {

    private static final Logger log = LoggerFactory.getLogger(OcrClient.class);

    private final RestClient restClient;

    public OcrClient(@Value("${app.ocr.sidecar-url}") String sidecarUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(sidecarUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    @SuppressWarnings("unchecked")
    public String extractText(Path imagePath) {
        String boundary = "ExamAgentBoundary" + UUID.randomUUID();
        byte[] body;
        try {
            body = buildMultipartBody(boundary, imagePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read image for OCR: " + imagePath, e);
        }

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri("/ocr")
                    .contentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary))
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.error("OCR sidecar call failed", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OCR sidecar not reachable - is it running? (ocr-sidecar/README.md): " + e, e);
        }

        if (response == null || response.get("text") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OCR sidecar returned no text");
        }
        return (String) response.get("text");
    }

    private byte[] buildMultipartBody(String boundary, Path imagePath) throws IOException {
        String filename = imagePath.getFileName().toString();
        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeAscii(out, "--" + boundary + "\r\n");
        writeAscii(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n");
        writeAscii(out, "Content-Type: " + contentType + "\r\n\r\n");
        out.write(Files.readAllBytes(imagePath));
        writeAscii(out, "\r\n--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private void writeAscii(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.US_ASCII));
    }
}
