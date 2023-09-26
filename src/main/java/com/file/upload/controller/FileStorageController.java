package com.file.upload.controller;

import com.file.upload.config.FileStorageProperties;
import com.file.upload.response.FileResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/files")
public class FileStorageController {

    private final Path fileLocation;

    public FileStorageController(FileStorageProperties fileStorageProperties) {
        this.fileLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    @PostMapping
    public ResponseEntity<FileResponse> upload(@RequestParam("file") MultipartFile file, HttpServletRequest httpRequest) {

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            Path targetLocation = fileLocation.resolve(fileName).normalize();
            file.transferTo(targetLocation);


            String fileDownload = ServletUriComponentsBuilder.fromContextPath(httpRequest)
                    .path("/api/files/download/")
                    .path(fileName).toUriString();

            return ResponseEntity.ok().body(FileResponse.builder()
                    .fileName(fileName)
                    .uri(fileDownload)
                    .size(file.getSize())
                    .build());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("fileName") String fileName, HttpServletRequest request) {
        Path targetLocation = fileLocation.resolve(fileName).normalize();

        try {
            Resource resource = new UrlResource(targetLocation.toUri());
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename \"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping()
    public ResponseEntity<List<FileResponse>> getFiles() throws IOException {

        try {
            List<FileResponse> files = Files.list(fileLocation)
                    .map(f -> {
                        try {
                            return FileResponse.builder()
                                    .fileName(f.getFileName().toString())
                                    .uri("/api/files/download/" + f.getFileName().toString())
                                    .size(Files.size(fileLocation.resolve(f.getFileName()).toAbsolutePath()))
                                    .build();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok()
                    .body(files);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
