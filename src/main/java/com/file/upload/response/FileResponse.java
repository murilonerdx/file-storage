package com.file.upload.response;

import lombok.Builder;

@Builder
public record FileResponse(String fileName, String uri, Long size){}