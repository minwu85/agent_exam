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
    private final Path uploadDir;

    public LectureService(LectureRepository lectureRepository, @Value("${app.upload-dir}") String uploadDir) {
        this.lectureRepository = lectureRepository;
        this.uploadDir = Path.of(uploadDir);
    }

    public Lecture uploadAndExtract(MultipartFile file, String title) throws IOException {
        Files.createDirectories(uploadDir);
        String storedFilename = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        Path storedPath = uploadDir.resolve(storedFilename);
        file.transferTo(storedPath);

        String rawText = extractText(storedPath);

        Lecture lecture = new Lecture(title, file.getOriginalFilename(), rawText);
        return lectureRepository.save(lecture);
    }

    private String extractText(Path pdfPath) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(new FileSystemResource(pdfPath));
        List<Document> pages = reader.get();
        return pages.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n"));
    }
}
