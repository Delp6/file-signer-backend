package com.delp6.filesignerbackend.service.impl;

import com.delp6.filesignerbackend.dto.FileDto;
import com.delp6.filesignerbackend.model.request.FileRequest;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;


class FileServiceImplTest {

    final String NAME = "name";
    final Path PATH = Paths.get("path");
    final String USER = "user";
    final String HASH = "hash";
    final String CONTENT = "content";
    final int LEADING = 20;
    final int NEW_LINE_AT_OFFSET = 20;
    @InjectMocks
    @Spy
    FileServiceImpl fileService;
    @Mock
    PDDocument pdDocument = new PDDocument();
    PDDocument pdDocument1;
    PDPage pdPage;
    FileDto fileDto;
    FileRequest fileRequest;
    MockMultipartFile mockMultipartFile;
    PDRectangle pdRectangle;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        pdRectangle = new PDRectangle(500, 500);
        pdPage = new PDPage(pdRectangle);
        pdDocument1 = new PDDocument();
        pdDocument1.addPage(pdPage);
        try {
            PDPageContentStream contentStream = new PDPageContentStream(pdDocument1, pdPage);
            contentStream.beginText();
            contentStream.newLineAtOffset(LEADING, NEW_LINE_AT_OFFSET);
            contentStream.setLeading(LEADING);
            contentStream.setFont(PDType1Font.TIMES_ROMAN, 20);
            contentStream.showText("text");
            contentStream.endText();
            contentStream.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        mockMultipartFile = new MockMultipartFile("name.pdf", "name.pdf",
                "application/pdf", (RandomStringUtils.randomAlphanumeric(20000)).getBytes());
        fileDto = FileDto.builder()
                .name(NAME)
                .user(USER)
                .path(PATH.toString())
                .build();
        fileRequest = new FileRequest(USER);
        pdDocument1 = new PDDocument();
        pdDocument1.addPage(pdPage);
    }

    @Test
    void should_edit_file() {
        doNothing().when(fileService).validateFile(any());
        doReturn(pdDocument).when(fileService).addDataPage(any(), anyString(), anyString());
        doReturn(pdDocument).when(fileService).convertMultiPartFileToPdf(any());
        doReturn(PATH).when(fileService).createDirectory();
        try {
            doNothing().when(pdDocument).save(fileDto.getPath());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        FileDto returnValue = fileService.editFile(mockMultipartFile, fileRequest);
        assertNotNull(returnValue);
        assertEquals(returnValue.getUser(), USER);
    }

    @Test
    void should_validate_file() {
        assertDoesNotThrow(() -> fileService.validateFile(mockMultipartFile));
    }

    @Test
    void should_add_data_page() {
        doReturn(Map.ofEntries(Map.entry("width", 100.0F),
                Map.entry("height", 100.0F))).when(fileService).calcNewPageSize(any());
        doReturn(HASH).when(fileService).generateFileHash(any(), any(), any(), any());
        PDDocument pdDocument2 = fileService.addDataPage(pdDocument1, USER, NAME);
        assertEquals(2, pdDocument2.getNumberOfPages());
    }

    @Test
    void should_generate_file_hash() {
        List<String> returnValue = new ArrayList<>();
        doReturn(CONTENT).when(fileService).getFileContent(any());
        for (int i = 0; i <= 10; i++) {
            returnValue.add(fileService.generateFileHash(pdDocument, USER, UUID.randomUUID(), NAME));
        }
        for (String s : returnValue) {
            assertEquals(64, s.length());
        }
        for (int i = 1; i < returnValue.size(); i++) {
            assertNotEquals(returnValue.get(i), returnValue.get(i - 1));
        }
    }

    @Test
    void should_get_file_content() {
        String returnValue = fileService.getFileContent(pdDocument1);
        assertNotEquals("<>", returnValue);
    }

    @Test
    void should_calc_hash_text_width() {
        float returnValue = fileService.calcHashTextWidth(HASH, PDType1Font.TIMES_ROMAN, 20);
        assertTrue(returnValue > 0F);
    }

    @Test
    void should_calc_new_page_size() {
        Map<String, Float> returnValue = fileService.calcNewPageSize(pdPage);
        assertTrue(returnValue.containsKey("width"));
        assertTrue(returnValue.containsKey("height"));
        assertTrue(returnValue.get("width") > 0);
        assertTrue(returnValue.get("height") > 0);
    }

    @Test
    void should_remove_temporary_file() {
        File file = new File(FileSystems.getDefault()
                .getPath("") + "test.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        assertTrue(fileService.removeTemporaryFile("test.txt"));
    }
}