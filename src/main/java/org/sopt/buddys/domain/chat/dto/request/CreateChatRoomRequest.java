package org.sopt.buddys.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateChatRoomRequest(
    @Schema(description = "채팅방을 생성할 상대방 사용자 ID", example = "2")
    @NotNull
    Long participantUserId
) {
}
