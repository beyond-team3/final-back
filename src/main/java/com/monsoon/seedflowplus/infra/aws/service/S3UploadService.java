package com.monsoon.seedflowplus.infra.aws.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    // Spring Cloud AWS가 제공하는 S3 전용 템플릿 객체
    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * 프론트엔드에서 전달받은 MultipartFile을 S3에 업로드하고 URL을 반환.
     */
    public String uploadProductImage(MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new CoreException(ErrorType.FILE_NOT_FOUND);
        }

        // 원본 파일명에서 확장자 추출 (예: watermelon.jpg -> .jpg)
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);

        // 파일 덮어쓰기 방지를 위한 고유한 파일명 생성 (예: 123e4567-e89b-12d3-a456-426614174000.jpg)
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;

        // S3 폴더 경로 설정 - 상품 이미지만 따로 모아두기 위해 "products/" 폴더 아래에 저장
        String s3Key = "products/" + uniqueFilename;

        try (InputStream inputStream = file.getInputStream()) {
            // 브라우저에서 사진이 다운로드되지 않고 곧바로 보이도록 Content-Type 지정
            ObjectMetadata metadata = ObjectMetadata.builder()
                    .contentType(file.getContentType())
                    .build();

            // S3에 파일 업로드 실행
            s3Template.upload(bucketName, s3Key, inputStream, metadata);

        } catch (IOException e) {
            throw new CoreException(ErrorType.DEFAULT_ERROR); // "이미지 업로드 실패" 에러로 변경 추천
        }

        // 업로드된 이미지의 공개 URL을 생성하여 반환
        return getPublicUrl(s3Key);
    }

    /**
     * S3에 저장된 객체의 퍼블릭 URL을 만들어주는 헬퍼 메서드
     */
    private String getPublicUrl(String s3Key) {
        return String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucketName, s3Key);
    }
}