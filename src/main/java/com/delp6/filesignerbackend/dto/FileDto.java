package com.delp6.filesignerbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDto {

    private UUID uuid;
    private String name;
    private String user;
    private Integer hash;
    private String path;

}
