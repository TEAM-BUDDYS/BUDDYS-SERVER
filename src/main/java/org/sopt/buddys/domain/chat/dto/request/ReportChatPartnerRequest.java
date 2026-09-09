package org.sopt.buddys.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ReportChatPartnerRequest(
    @Schema(
        description = "신고 사유. 현재는 별도 사유 입력 없이 신고할 수 있어 생략 가능합니다.",
        example = "부적절한 언행",
        nullable = true
    )
    @Size(max = 500)
    String reason
) {
}
