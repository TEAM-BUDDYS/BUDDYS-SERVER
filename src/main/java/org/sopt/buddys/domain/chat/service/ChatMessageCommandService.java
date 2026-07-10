package org.sopt.buddys.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatMessage;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.entity.ChatRoomMemberId;
import org.sopt.buddys.domain.chat.repository.ChatMessageRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.service.result.ChatMessageSendResult;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageCommandService {

  private final ChatMessageRepository chatMessageRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomMemberRepository chatRoomMemberRepository;
  private final UserRepository userRepository;

  @Transactional
  public ChatMessageSendResult sendMessage(
      Long userId,
      Long chatRoomId,
      String content
  ) {

    User sender = getActiveUser(userId);
    ChatRoom chatRoom = getAccessibleChatRoom(userId, chatRoomId);
    ChatMessage message = chatMessageRepository.save(
        new ChatMessage(chatRoom, sender, content)
    );

    return new ChatMessageSendResult(message);
  }

  private User getActiveUser(Long userId) {
    return userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
  }

  private ChatRoom getAccessibleChatRoom(
      Long userId,
      Long chatRoomId
  ) {

    if (chatRoomMemberRepository.existsById(new ChatRoomMemberId(chatRoomId, userId))) {
      return chatRoomRepository.getReferenceById(chatRoomId);
    }

    if (!chatRoomRepository.existsById(chatRoomId)) {
      throw new BaseException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    throw new BaseException(GlobalErrorCode.FORBIDDEN);
  }
}
