package com.delp6.filesignerbackend.service;

import com.delp6.filesignerbackend.dto.FileDto;
import com.delp6.filesignerbackend.model.request.FileRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public interface FileService {

    FileDto editFile(MultipartFile file, FileRequest userName);

    void validateFile(MultipartFile file);

    PDDocument convertMultiPartFileToPdf(MultipartFile multipartFile);

    PDDocument addDataPage(PDDocument pdDocument, String user, String fileName);

    String generateFileHash(PDDocument pdDocument, String user, UUID uuid, String fileName);

    String getFileContent(PDDocument pdDocument);

    Float calcHashTextWidth(String hash, PDFont font, int fontSize);

    Map<String, Float> calcNewPageSize(PDPage pdPage);

    Path createDirectory();

    Boolean checkFileAuthentication(MultipartFile file);

    String getHash(PDPage pdPage);

    String getUser(PDPage pdPage);

    UUID getUuid(PDPage pdPage);
}
