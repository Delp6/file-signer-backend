package com.delp6.filesignerbackend.service.impl;


import com.delp6.filesignerbackend.dto.FileDto;
import com.delp6.filesignerbackend.exception.ApiErrorMessages;
import com.delp6.filesignerbackend.exception.ApiException;
import com.delp6.filesignerbackend.model.request.FileRequest;
import com.delp6.filesignerbackend.property.FileStorageProperties;
import com.delp6.filesignerbackend.service.FileService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private final FileStorageProperties fileStorageProperties;

    public FileServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public FileDto editFile(MultipartFile multipartFile, FileRequest fileRequest) {
        validateFile(multipartFile);
        try {
            PDDocument pdDocument = addDataPage(convertMultiPartFileToPdf(multipartFile),
                    fileRequest.getUser(), multipartFile.getOriginalFilename());
            Path uploadPath = createDirectory();
            FileDto fileDto = FileDto
                    .builder()
                    .name(StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename())))
                    .user(fileRequest.getUser())
                    .path(uploadPath + "/" + multipartFile.getOriginalFilename())
                    .build();
            pdDocument.save(fileDto.getPath());
            pdDocument.close();
            return fileDto;

        } catch (IOException ex) {
            throw new ApiException(ApiErrorMessages.COULD_NOT_STORE_FILE.getErrorMessage());
        }
    }

    @Override
    public void validateFile(MultipartFile multipartFile) {

        if (multipartFile.getName().contains("..")) {
            throw new ApiException(ApiErrorMessages
                    .FILE_NAME_CONTAINS_INVALID_PATH.getErrorMessage() + " " + multipartFile.getName());
        }
        if (!Objects.equals(multipartFile.getContentType(), "application/pdf")) {
            throw new ApiException(ApiErrorMessages
                    .ILLEGAL_FILE_FORMAT.getErrorMessage() + " " + multipartFile.getContentType());
        }
        /*
         * MAX SIZE: 100mb
         */
        if (multipartFile.getSize() > 100000000) {
            throw new ApiException(ApiErrorMessages
                    .ILLEGAL_FILE_SIZE.getErrorMessage());
        }
    }

    @Override
    public PDDocument convertMultiPartFileToPdf(MultipartFile multipartFile) {
        try {
            File file = new File(Objects.requireNonNull(multipartFile.getOriginalFilename()));
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(multipartFile.getBytes());
            fos.close();
            return PDDocument.load(file);
        } catch (IOException ioException) {
            throw new ApiException(ApiErrorMessages
                    .COULD_NOT_CONVERT_MULTI_PART_FILE_TO_PDF_FILE.getErrorMessage());
        }
    }

    @Override
    public PDDocument addDataPage(PDDocument pdDocument, String user, String fileName) {

        PDFont font = PDType1Font.TIMES_ROMAN;
        int fontSize = 14;
        int leading = (int) ((Math.round(fontSize / 10.0) + 1) * 10);
        final UUID RANDOM_UUID = UUID.randomUUID();
        final String HASH = generateFileHash(pdDocument, user, RANDOM_UUID, fileName);

        float hashTextWidth = calcHashTextWidth(HASH, font, fontSize);
        float width = calcNewPageSize(pdDocument.getPage(0)).get("width");
        float height = calcNewPageSize(pdDocument.getPage(0)).get("height");
        while (hashTextWidth + 40 > width) {
            fontSize = fontSize - 1;
            hashTextWidth = calcHashTextWidth(HASH, font, fontSize);
            leading = (int) ((Math.round(fontSize / 10.0) + 1) * 10);
        }
        int newLineYOffset = (int) (height - (leading * 2));
        PDRectangle pdRectangle = new PDRectangle(width, height);
        PDPage pdPage = new PDPage(pdRectangle);
        pdDocument.addPage(pdPage);

        try {
            PDPageContentStream contents = new PDPageContentStream(pdDocument, pdPage);
            contents.beginText();
            contents.setFont(font, fontSize);
            contents.newLineAtOffset(leading, newLineYOffset);
            contents.setLeading(leading);
            contents.showText("User: " + user);
            contents.newLine();
            contents.showText("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            contents.newLine();
            contents.showText("Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            contents.newLine();
            contents.showText("Time Zone: " + TimeZone.getDefault().getID());
            contents.newLine();
            contents.showText("Hash: " + HASH);
            contents.newLine();
            contents.showText("Uuid: " + RANDOM_UUID);
            contents.endText();
            contents.close();
            return pdDocument;

        } catch (IOException ioException) {
            throw new ApiException(ApiErrorMessages.COULD_NOT_ADD_DATA.getErrorMessage());
        }
    }

    @Override
    public String generateFileHash(PDDocument pdDocument, String user, UUID uuid, String fileName) {
        return DigestUtils.sha256Hex(user + uuid + getFileContent(pdDocument) + fileName);
    }

    @Override
    public String getFileContent(PDDocument pdDocument) {

        String returnValue = "";
        try {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            pdfTextStripper.setStartPage(1);
            pdfTextStripper.setEndPage(pdDocument.getNumberOfPages() - 1);
            for (PDPage page : pdDocument.getPages()) {
                for (COSName xObjectName : page.getResources().getXObjectNames()) {
                    page.getResources().getXObject(xObjectName);
                    returnValue = returnValue.concat(String.valueOf(page.getResources().
                            getXObject(xObjectName).getStream().getCOSObject().toTextString()));
                }
            }
            return returnValue.concat(pdfTextStripper.getText(pdDocument));

        } catch (IOException ioException) {
            throw new ApiException(ApiErrorMessages.COULD_NOT_GET_PDF_CONTENT.getErrorMessage());
        }
    }

    @Override
    public Float calcHashTextWidth(String hash, PDFont font, int fontSize) {
        try {
            return font.getStringWidth("Hash: " + hash) / 1000 * fontSize;
        } catch (IOException ioException) {
            throw new ApiException(ApiErrorMessages.COULD_NOT_CALC_HASH_TEXT_WIDTH.getErrorMessage());

        }
    }

    @Override
    public Map<String, Float> calcNewPageSize(PDPage pdPage) {
        return Map.ofEntries(Map.entry("width", pdPage.getMediaBox().getWidth()),
                Map.entry("height", pdPage.getMediaBox().getHeight()));
    }

    @Override
    public Path createDirectory() {
        try {
            Path path = Paths.get(fileStorageProperties.getUploadDir()
                    + RandomStringUtils.randomAlphanumeric(64) + LocalDateTime.now()
                    .toString().replaceAll(":", "."));
            return Files.createDirectories(path);
        } catch (IOException ioException) {
            throw new ApiException(ApiErrorMessages.COULD_NOT_CREATE_FILE_DIRECTORY.getErrorMessage());
        }
    }

    @Override
    public Boolean checkFileAuthentication(MultipartFile file) {
        validateFile(file);
        PDDocument pdDocument = convertMultiPartFileToPdf(file);
        if (pdDocument.getNumberOfPages() == 1) {
            throw new ApiException(ApiErrorMessages.COULD_NOT_VALIDATE_FILE.getErrorMessage());
        }
        PDPage page = pdDocument.getPage(pdDocument.getNumberOfPages() - 1);
        pdDocument.removePage(page);

        return  getHash(page).equals(generateFileHash(pdDocument, getUser(page), getUuid(page),
                file.getOriginalFilename()));
    }

    @Override
    public String getHash(PDPage pdPage) {
        try {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            PDDocument pdDocument = new PDDocument();
            pdDocument.addPage(pdPage);
            String text = pdfTextStripper.getText(pdDocument);
            pdDocument.close();
            return text.substring(text.indexOf("Hash:") + 6, text.indexOf("Hash:") + 70);
        } catch (IOException ioException) {
            throw new ApiException(ApiErrorMessages.COULD_NOT_EXTRACT_DATA.getErrorMessage());
        }
    }

    @Override
    public String getUser(PDPage pdPage) {
        try {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            PDDocument pdDocument = new PDDocument();
            pdDocument.addPage(pdPage);
            String text = pdfTextStripper.getText(pdDocument);
            pdDocument.close();
            return text.substring(text.indexOf("User:") + 6, text.indexOf("Date:")).trim();
        } catch (IOException ioException) {
            throw new ApiException(ApiErrorMessages.COULD_NOT_EXTRACT_DATA.getErrorMessage());
        }
    }

    @Override
    public UUID getUuid(PDPage pdPage) {
        try {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            PDDocument pdDocument = new PDDocument();
            pdDocument.addPage(pdPage);
            String text = pdfTextStripper.getText(pdDocument);
            pdDocument.close();
            return UUID.fromString(text.substring(text.indexOf("Uuid:") + 6, text.indexOf("Uuid:") + 42));
        } catch (IOException ioException) {
            throw new ApiException(ApiErrorMessages.COULD_NOT_EXTRACT_DATA.getErrorMessage());
        }
    }
}
