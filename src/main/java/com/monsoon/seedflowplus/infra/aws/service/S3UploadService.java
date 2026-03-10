package com.monsoon.seedflowplus.infra.aws.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {

    // Spring Cloud AWS가 제공하는 S3 전용 템플릿 객체
    private final S3Template s3Template;
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${spring.cloud.aws.region.static:ap-northeast-2}")
    private String region;

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");
    /**
     * 프론트엔드에서 전달받은 MultipartFile을 S3에 업로드하고 URL을 반환.
     */
    public String uploadProductImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new CoreException(ErrorType.INVALID_FILE_UPLOAD);
        }

        // 파일명과 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);

        // 확장자 검증 (화이트리스트에 없는 파일이면 에러 발생!)
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new CoreException(ErrorType.INVALID_FILE_FORMAT);
        }

        // MIME 타입 한 번 더 검증 (확장자만 .jpg로 바꾼 가짜 파일 방어)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CoreException(ErrorType.INVALID_FILE_FORMAT);
        }

        // 파일명 생성 (UUID)
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
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
     * S3 이미지 삭제 메서드 (보상 트랜잭션용)
     * URL을 통째로 넘겨주면, 내부에서 S3 Key를 추출해 삭제합니다.
     */
    public void deleteImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String urlPrefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);

            // 이미지 URL이 우리 S3 버킷 URL이 맞는지 확인 후 삭제
            if (imageUrl.startsWith(urlPrefix)) {
                String s3Key = imageUrl.substring(urlPrefix.length()); // "products/abcd.jpg" 부분만 추출
                s3Template.deleteObject(bucketName, s3Key);
                log.info("고아 객체 삭제 완료: {}", s3Key);
            }
        } catch (Exception e) {
            log.error("S3 이미지 삭제 실패: {}", imageUrl, e);
        }
    }

    /**
     * S3에 저장된 객체의 퍼블릭 URL을 만들어주는 헬퍼 메서드
     */
    private String getPublicUrl(String s3Key) {
        return s3Client.utilities().getUrl(builder -> builder
                        .bucket(bucketName)
                        .key(s3Key))
                .toExternalForm();
    }
}