package com.delp6.filesignerbackend.controller;

import com.delp6.filesignerbackend.dto.FileDto;
import com.delp6.filesignerbackend.model.request.FileRequest;
import com.delp6.filesignerbackend.model.response.FileResponse;
import com.delp6.filesignerbackend.service.FileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    /*TODO CHANGE THIS TO @REQUESTPART*/
    public FileResponse uploadFile(@RequestParam("file") MultipartFile file, @RequestHeader("user") String userName) {
        FileDto fileDto = fileService.editFile(file, new FileRequest(userName));

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(fileDto.getPath())
                .toUriString();

        return new FileResponse(fileDto.getPath(), fileDownloadUri);
    }
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @RequestMapping(path = "/validate")
    public Boolean validateFile(@RequestParam("file") MultipartFile file) {
        return fileService.checkFileAuthentication(file);
    }
}
