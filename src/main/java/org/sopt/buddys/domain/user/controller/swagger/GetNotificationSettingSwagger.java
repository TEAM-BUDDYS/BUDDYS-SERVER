package org.sopt.buddys.domain.user.controller.swagger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sopt.buddys.global.swagger.CommonErrorResponses;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(summary = "알림 설정 조회", description = "로그인한 사용자의 알림 설정 여부를 조회합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "알림 설정 조회 성공")
})
@UserNotFoundResponse
@CommonErrorResponses
public @interface GetNotificationSettingSwagger {
}
