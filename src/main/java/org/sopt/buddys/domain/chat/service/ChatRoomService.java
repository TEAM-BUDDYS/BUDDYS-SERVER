package org.sopt.buddys.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.service.result.ChatRoomResult;
import org.sopt.buddys.domain.chat.util.DirectChatKey;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

  private static final String DIRECT_CHAT_KEY_UNIQUE_CONSTRAINT = "uk_chat_room_direct_chat_key";
  private static final int DIRECT_CHAT_ROOM_RETRY_COUNT = 3;
  private static final long DIRECT_CHAT_ROOM_RETRY_INTERVAL_MILLIS = 50L;

  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomCommandService chatRoomCommandService;
  private final UserRepository userRepository;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public ChatRoomResult createOrGetChatRoom(
      Long userId,
      Long participantUserId
  ) {

    if (userId.equals(participantUserId)) {
      throw new BaseException(ChatErrorCode.CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
    }

    validateUserExists(userId);
    User participant = getActiveUser(participantUserId);
    String directChatKey = DirectChatKey.from(userId, participantUserId);

    ChatRoom chatRoom = chatRoomRepository.findByDirectChatKey(directChatKey)
        .orElseGet(() -> createChatRoom(userId, participantUserId, directChatKey));

    return new ChatRoomResult(chatRoom, participant);
  }

  private void validateUserExists(Long userId) {
    if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
      throw new BaseException(UserErrorCode.USER_NOT_FOUND);
    }
  }

  private User getActiveUser(Long userId) {
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
      if (!isDirectChatKeyDuplicate(e)) {
        throw e;
      }
      return findDirectChatRoomAfterDuplicateKey(directChatKey, e);
    }
  }

  private boolean isDirectChatKeyDuplicate(DataIntegrityViolationException exception) {
    Throwable cause = exception;

    while (cause != null) {
      if (isDirectChatKeyConstraint(cause) || hasDirectChatKeyConstraintMessage(cause)) {
        return true;
      }
      cause = cause.getCause();
    }

    return false;
  }

  private boolean isDirectChatKeyConstraint(Throwable cause) {
    if (cause instanceof ConstraintViolationException constraintViolationException) {
      return DIRECT_CHAT_KEY_UNIQUE_CONSTRAINT.equals(
          constraintViolationException.getConstraintName()
      );
    }
    return false;
  }

  private boolean hasDirectChatKeyConstraintMessage(Throwable cause) {
    String message = cause.getMessage();
    return message != null && message.contains(DIRECT_CHAT_KEY_UNIQUE_CONSTRAINT);
  }

  private ChatRoom findDirectChatRoomAfterDuplicateKey(
      String directChatKey,
      DataIntegrityViolationException exception
  ) {

    for (int i = 0; i < DIRECT_CHAT_ROOM_RETRY_COUNT; i++) {
      ChatRoom chatRoom = chatRoomRepository.findByDirectChatKey(directChatKey)
          .orElse(null);

      if (chatRoom != null) {
        return chatRoom;
      }

      sleepBeforeRetry();
    }

    throw exception;
  }

  private void sleepBeforeRetry() {
    try {
      Thread.sleep(DIRECT_CHAT_ROOM_RETRY_INTERVAL_MILLIS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

}
