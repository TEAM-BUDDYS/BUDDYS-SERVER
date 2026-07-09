package org.sopt.buddys.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatRoomMember;
import org.sopt.buddys.domain.chat.entity.ChatRoomMemberId;
import org.sopt.buddys.domain.chat.repository.ChatMessageRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.service.result.ChatReadResult;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatReadService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomMemberRepository chatRoomMemberRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final UserRepository userRepository;

  @Transactional
  public ChatReadResult markAsRead(
      Long userId,
      Long chatRoomId,
      Long lastReadMessageId
  ) {

    validateUserExists(userId);
    validateLastReadMessageId(lastReadMessageId);

    ChatRoomMember chatRoomMember = getChatRoomMember(userId, chatRoomId);
    validateLastReadMessage(chatRoomId, lastReadMessageId);
    chatRoomMember.updateLastReadMessageId(lastReadMessageId);

    return new ChatReadResult(
        chatRoomId,
        userId,
        chatRoomMember.getLastReadMessageId()
    );
  }

  private void validateUserExists(Long userId) {
    if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
      throw new BaseException(UserErrorCode.USER_NOT_FOUND);
    }
  }

  private void validateLastReadMessageId(
      Long lastReadMessageId
  ) {

    if (lastReadMessageId == null) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }

  private void validateLastReadMessage(
      Long chatRoomId,
      Long lastReadMessageId
  ) {

    if (!chatMessageRepository.existsByIdAndChatRoomId(lastReadMessageId, chatRoomId)) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }

  private ChatRoomMember getChatRoomMember(
      Long userId,
      Long chatRoomId
  ) {

    ChatRoomMemberId chatRoomMemberId = new ChatRoomMemberId(chatRoomId, userId);

    return chatRoomMemberRepository.findById(chatRoomMemberId)
        .orElseThrow(() -> {
          if (!chatRoomRepository.existsById(chatRoomId)) {
            return new BaseException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
          }
          return new BaseException(GlobalErrorCode.FORBIDDEN);
        });
  }
}
