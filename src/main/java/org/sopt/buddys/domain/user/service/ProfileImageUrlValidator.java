package org.sopt.buddys.domain.user.service;

import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.image.entity.ImageDomain;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Component;

/**
 * 프로필 이미지 URL은 다른 사용자 화면에 로드되므로, 프로필 이미지 업로드 경로의 URL만 새로 설정할 수 있다.
 * 소셜 로그인으로 받은 외부 이미지처럼 이미 설정된 값은 그대로 유지할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class ProfileImageUrlValidator {

  private static final Pattern UPLOADED_FILE_NAME = Pattern.compile("^[A-Za-z0-9-]+\\.[A-Za-z0-9]+$");

  private final S3PresignedUrlManager s3PresignedUrlManager;

  public void validate(String requestedUrl, String currentUrl) {
    if (requestedUrl == null || requestedUrl.equals(currentUrl)) {
      return;
    }

    String uploadUrlPrefix = s3PresignedUrlManager.buildPublicUrl(
        ImageDomain.PROFILE.getFolder() + "/"
    );
    if (!requestedUrl.startsWith(uploadUrlPrefix)
        || !UPLOADED_FILE_NAME.matcher(requestedUrl.substring(uploadUrlPrefix.length())).matches()) {
      throw new BaseException(UserErrorCode.INVALID_PROFILE_IMAGE_URL);
    }
  }
}
