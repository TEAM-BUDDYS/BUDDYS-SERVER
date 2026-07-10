package org.sopt.buddys.domain.chat.service.result;

import org.sopt.buddys.domain.chat.entity.ChatMessage;

public record ChatMessageSendResult(
    ChatMessage message
) {
}
