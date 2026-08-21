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
@Operation(
    summary = "회원 탈퇴",
    description = "로그인한 회원을 탈퇴 상태로 변경하고 리프레시 토큰을 폐기합니다. 작성한 콘텐츠는 유지됩니다."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
})
@CommonErrorResponses
public @interface WithdrawUserSwagger {
}
