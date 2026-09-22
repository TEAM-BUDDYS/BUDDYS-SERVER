package org.sopt.buddys.global.aws.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

class S3ObjectManagerTest {

  @DisplayName("객체 키로 지정한 버킷의 S3 객체를 삭제한다")
  @Test
  void delete_deletesObjectFromConfiguredBucket() {
    // given
    S3Client s3Client = mock(S3Client.class);
    S3Properties s3Properties = mock(S3Properties.class);
    when(s3Properties.getBucket()).thenReturn("buddys-test-bucket");
    S3ObjectManager manager = new S3ObjectManager(s3Client, s3Properties);

    // when
    manager.delete("exchange-verifications/7/old-document.pdf");

    // then
    ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(
        DeleteObjectRequest.class
    );
    verify(s3Client).deleteObject(requestCaptor.capture());
    assertThat(requestCaptor.getValue().bucket()).isEqualTo("buddys-test-bucket");
    assertThat(requestCaptor.getValue().key())
        .isEqualTo("exchange-verifications/7/old-document.pdf");
  }
}
