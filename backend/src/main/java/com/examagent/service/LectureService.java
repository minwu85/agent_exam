package com.examagent.service;

import com.examagent.model.Lecture;
import com.examagent.repository.LectureRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LectureService {

    private final LectureRepository lectureRepository;
    private final OcrClient ocrClient;
    private final Path uploadDir;

    public LectureService(LectureRepository lectureRepository, OcrClient ocrClient,
                           @Value("${app.upload-dir}") String uploadDir) {
        this.lectureRepository = lectureRepository;
        this.ocrClient = ocrClient;
        this.uploadDir = Path.of(uploadDir);
    }

    /** Stage 1: text-native PDFs, extracted directly in Java via Spring AI's PagePdfDocumentReader - no OCR involved. */
    public Lecture uploadAndExtract(MultipartFile file, String title) throws IOException {
        Path storedPath = save(file);
        String rawText = extractPdfText(storedPath);
        return saveLecture(title, file.getOriginalFilename(), rawText);
    }

    /** Stage 10: scanned/handwritten note images, routed through the Python OCR sidecar - see OcrClient. */
    public Lecture uploadScanAndExtract(MultipartFile file, String title) throws IOException {
        Path storedPath = save(file);
        String rawText = ocrClient.extractText(storedPath);
        return saveLecture(title, file.getOriginalFilename(), rawText);
    }

    private Path save(MultipartFile file) throws IOException {
        Files.createDirectories(uploadDir);
        String storedFilename = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        Path storedPath = uploadDir.resolve(storedFilename);
        file.transferTo(storedPath);
        return storedPath;
    }

    private Lecture saveLecture(String title, String originalFilename, String rawText) {
        Lecture lecture = new Lecture(title, originalFilename, rawText);
        return lectureRepository.save(lecture);
    }

    private String extractPdfText(Path pdfPath) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(new FileSystemResource(pdfPath));
        List<Document> pages = reader.get();
        return pages.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n"));
    }
}
