package com.noworld.notemap.data.dto;

public class OssPresignRequest {
    public String fileName;
    public long fileSize;
    public String mimeType;

    public OssPresignRequest(String fileName, long fileSize, String mimeType) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }
}
