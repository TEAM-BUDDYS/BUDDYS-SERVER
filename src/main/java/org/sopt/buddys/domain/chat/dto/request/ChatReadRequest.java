package org.sopt.buddys.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChatReadRequest(
    @NotNull(message = "마지막으로 읽은 메시지 ID를 입력해주세요.")
    Long lastReadMessageId
) {
}
