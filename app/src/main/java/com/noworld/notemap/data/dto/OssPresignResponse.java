package com.noworld.notemap.data.dto;

import java.util.Map;

public class    OssPresignResponse {
    public String uploadUrl; // Presigned upload URL
    public String fileUrl;   // Public URL after upload
    public Map<String, String> headers; // Headers required for upload, optional
}
