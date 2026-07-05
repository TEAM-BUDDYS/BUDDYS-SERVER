package org.sopt.buddys.domain.chat.service.result;

import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.user.entity.User;

public record ChatRoomResult(
    ChatRoom chatRoom,
    User participant
) {
}
