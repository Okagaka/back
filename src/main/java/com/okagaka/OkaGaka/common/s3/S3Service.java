package com.okagaka.OkaGaka.common.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.Map;

// 삭제
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class S3Service {
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String region;

    private final List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/gif");

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    private static final Map<String, String> mimeTypeToExtension = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif"
    );

    private String getExtensionFromMimeType(String mimeType) {
        String extension = mimeTypeToExtension.get(mimeType);
        if (extension == null) {
            throw new IllegalArgumentException("지원하지 않는 MIME 타입입니다: " + mimeType);
        }
        return extension;
    }


//    public String uploadImage(MultipartFile file) throws IOException {
//        validateFile(file);
//
//        String fileName = UUID.randomUUID() + getExtension(file.getOriginalFilename());
//
//        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                .bucket(bucketName)
//                .key(fileName)
//                .contentType(file.getContentType())
//                .build();
//
//        s3Client.putObject(putObjectRequest,
//                software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));
//
//        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + fileName;
//    }

    public String uploadImage(MultipartFile file) throws IOException {
        validateFile(file);

        String mimeType = file.getContentType();
        String extension = getExtensionFromMimeType(mimeType);

        String fileName = UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(mimeType)
                .build();

//        s3Client.putObject(putObjectRequest,
//                software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));
        try {
            s3Client.putObject(putObjectRequest,
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));
        } catch (Exception e) {
            logger.error("S3 업로드 중 오류 발생: {}", e.getMessage(), e); // 스택트레이스까지 출력됨
            throw e;
        }


        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + fileName;
    }


    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        if (!allowedMimeTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only image files are allowed (jpeg, png, gif).");
        }
    }

    private String getExtension(String originalName) {
        int dotIndex = originalName.lastIndexOf(".");
        return dotIndex != -1 ? originalName.substring(dotIndex) : "";
    }
}