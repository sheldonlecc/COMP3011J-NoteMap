package com.noworld.notemap.data.dto;

import java.util.Map;

public class    OssPresignResponse {
    public String uploadUrl; // 带签名的上传地址
    public String fileUrl;   // 上传后可访问的 URL
    public Map<String, String> headers; // 需要随上传一起带的 Header，可为空
}
