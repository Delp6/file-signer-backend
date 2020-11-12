package com.delp6.filesignerbackend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApiErrorMessages {

    COULD_NOT_CREATE_DIRECTORY("Could not create the directory where the uploaded files will be stored."),
    FILE_NAME_CONTAINS_INVALID_PATH("Filename contains invalid path sequence."),
    COULD_NOT_STORE_FILE("Could not store file"),
    ILLEGAL_FILE_FORMAT("File has illegal file format"),
    COULD_NOT_CONVERT_MULTI_PART_FILE_TO_PDF_FILE("Could not convert multiPartFile to pdf file."),
    ILLEGAL_FILE_SIZE("File is too big to upload."),
    ILLEGAL_FILE_UUID("UUID already exists."),
    COULD_NOT_ADD_DATA("Could not add required data to file."),
    COULD_NOT_GET_PDF_CONTENT("Could not get pdf file content."),
    COULD_NOT_CALC_HASH_TEXT_WIDTH("Could not calculate hash text width."),
    COULD_NOT_VALIDATE_FILE("Could not validate file."),
    COULD_NOT_EXTRACT_DATA("Could not extract data from file."),
    COULD_NOT_CREATE_FILE_DIRECTORY("Could not create directory.");

    private final String errorMessage;
}

