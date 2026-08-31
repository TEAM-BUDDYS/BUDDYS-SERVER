package org.sopt.buddys.domain.user.controller.swagger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(summary = "알림 설정 변경", description = "로그인한 사용자의 알림 설정 여부를 변경합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "알림 설정 변경 성공")
})
@InvalidRequestResponse
@UserNotFoundResponse
@CommonErrorResponses
public @interface UpdateNotificationSettingSwagger {
}
