package org.sopt.buddys.domain.chat.service.result;

public record ChatReadResult(
    Long chatRoomId,
    Long readerId,
    Long lastReadMessageId
) {
}
