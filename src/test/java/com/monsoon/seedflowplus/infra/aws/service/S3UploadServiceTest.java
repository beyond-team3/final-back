package com.monsoon.seedflowplus.infra.aws.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;

import java.io.InputStream;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3UploadServiceTest {

    @Mock
    private S3Template s3Template;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3UploadService s3UploadService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3UploadService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3UploadService, "region", "ap-northeast-2");
    }

    @Test
    @DisplayName("정상적인 이미지 파일을 업로드하면 S3 URL을 반환한다")
    void uploadProductImage_Success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "test image content".getBytes());

        S3Utilities mockS3Utilities = mock(S3Utilities.class);
        when(s3Client.utilities()).thenReturn(mockS3Utilities);

        URL mockUrl = new URL("https://test-bucket.s3.ap-northeast-2.amazonaws.com/products/test.png");
        when(mockS3Utilities.getUrl(any(java.util.function.Consumer.class))).thenReturn(mockUrl);

        // when
        String resultUrl = s3UploadService.uploadProductImage(file);

        // then
        assertThat(resultUrl).isEqualTo(mockUrl.toExternalForm());
        verify(s3Template, times(1)).upload(eq("test-bucket"), anyString(), any(InputStream.class),
                any(ObjectMetadata.class));
    }

    @Test
    @DisplayName("허용되지 않은 확장자 파일 업로드 시 예외가 발생한다")
    void uploadProductImage_InvalidExtension() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test script".getBytes());

        // when & then
        assertThatThrownBy(() -> s3UploadService.uploadProductImage(file))
                .isInstanceOf(CoreException.class);

        verify(s3Template, never()).upload(anyString(), anyString(), any(InputStream.class), any());
    }

    @Test
    @DisplayName("S3 URL이 주어지면 해당 객체를 삭제한다")
    void deleteImageFromUrl_Success() {
        // given
        String imageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/products/test-uuid.png";

        // when
        s3UploadService.deleteImageFromUrl(imageUrl);

        // then
        verify(s3Template, times(1)).deleteObject("test-bucket", "products/test-uuid.png");
    }

    @Test
    @DisplayName("잘못된 S3 URL이 주어지면 삭제를 수행하지 않는다")
    void deleteImageFromUrl_InvalidUrl() {
        // given
        String imageUrl = "https://other-bucket.s3.ap-northeast-2.amazonaws.com/products/test-uuid.png";

        // when
        s3UploadService.deleteImageFromUrl(imageUrl);

        // then
        verify(s3Template, never()).deleteObject(anyString(), anyString());
    }
}
