package org.sopt.buddys.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.service.result.ChatRoomResult;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomCommandService chatRoomCommandService;
  private final UserRepository userRepository;

  public ChatRoomResult createOrGetChatRoom(
      Long userId,
      Long participantUserId
  ) {

    if (userId.equals(participantUserId)) {
      throw new BaseException(ChatErrorCode.CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
    }

    User user = findActiveUser(userId);
    User participant = findActiveUser(participantUserId);
    String directChatKey = createDirectChatKey(userId, participantUserId);

    ChatRoom chatRoom = chatRoomRepository.findByDirectChatKey(directChatKey)
        .orElseGet(() -> createChatRoom(userId, participantUserId, directChatKey));

    return new ChatRoomResult(chatRoom, participant);
  }

  private User findActiveUser(Long userId) {
    return userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
  }

  private ChatRoom createChatRoom(
      Long userId,
      Long participantUserId,
      String directChatKey
  ) {

    try {
      return chatRoomCommandService.createDirectChatRoom(userId, participantUserId, directChatKey);
    } catch (DataIntegrityViolationException e) {
      return chatRoomRepository.findByDirectChatKey(directChatKey)
          .orElseThrow(() -> e);
    }
  }

  private String createDirectChatKey(
      Long userId,
      Long participantUserId
  ) {

    long firstUserId = Math.min(userId, participantUserId);
    long secondUserId = Math.max(userId, participantUserId);
    return "%d:%d".formatted(firstUserId, secondUserId);
  }
}
