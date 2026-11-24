package org.sopt.buddys.domain.image.service;

import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.image.entity.ImageDomain;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.springframework.stereotype.Component;

/**
 * 우리 S3 업로드 경로로 올라간 이미지 URL인지 판별한다.
 *
 * <p>업로드 URL은 {버킷 공개 URL}/{도메인 폴더}/{UUID}.{확장자} 형태다(S3PresignedUrlService).
 * 다른 사용자 화면에 노출되거나 리다이렉트 대상으로 쓰이는 URL은 이 형태만 허용해 임의 외부 URL을 막는다.
 */
@Component
@RequiredArgsConstructor
public class UploadedImageUrlMatcher {

  private static final Pattern UPLOADED_FILE_NAME = Pattern.compile("^[A-Za-z0-9-]+\\.[A-Za-z0-9]+$");

  private final S3PresignedUrlManager s3PresignedUrlManager;

  public boolean matches(String url, ImageDomain imageDomain) {
    if (url == null) {
      return false;
    }
    String uploadUrlPrefix = s3PresignedUrlManager.buildPublicUrl(imageDomain.getFolder() + "/");
    return url.startsWith(uploadUrlPrefix)
        && UPLOADED_FILE_NAME.matcher(url.substring(uploadUrlPrefix.length())).matches();
  }
}
