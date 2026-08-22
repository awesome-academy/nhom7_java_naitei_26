package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.exception.FileStorageException;
import com.nhom7.coworkingspace.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @Value("${app.supabase.key}")
    private String supabaseKey;

    @Value("${app.supabase.bucket:coworking-space}")
    private String bucketName;

    private final RestClient restClient = RestClient.create();

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String fileExtension = "";
            int extIndex = originalFilename.lastIndexOf(".");
            if (extIndex > 0) {
                fileExtension = originalFilename.substring(extIndex);
            }

            String uniqueFilename = UUID.randomUUID() + fileExtension;
            String filePath = subDirectory + "/" + uniqueFilename;

            String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, filePath);

            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            restClient.post()
                    .uri(uploadUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();

            return filePath;
        } catch (Exception ex) {
            log.error("Failed to upload file to Supabase storage in directory: {}", subDirectory, ex);
            throw new FileStorageException("common.error", ex);
        }
    }

    @Override
    public String createSignedUrl(String filePath, int expiresInSeconds) {
        try {
            String signUrl = String.format("%s/storage/v1/object/sign/%s/%s", supabaseUrl, bucketName, filePath);
            record SignRequest(int expiresIn) {
            }
            record SignResponse(String signedURL) {
            }

            SignResponse response = restClient.post()
                    .uri(signUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SignRequest(expiresInSeconds))
                    .retrieve()
                    .body(SignResponse.class);

            if (response != null && response.signedURL() != null) {
                // Supabase returns signedURL relative to /storage/v1 (e.g. "/object/sign/<bucket>/<path>?token=..."),
                // not relative to the project root - must prepend /storage/v1 or the URL 404s.
                return supabaseUrl + "/storage/v1" + response.signedURL();
            }
            return null;
        } catch (Exception ex) {
            log.error("Failed to generate signed URL for filePath: {}", filePath, ex);
            throw new FileStorageException("common.error", ex);
        }
    }
}
