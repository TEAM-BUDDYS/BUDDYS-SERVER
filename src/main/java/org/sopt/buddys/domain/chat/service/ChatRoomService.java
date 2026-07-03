package org.sopt.buddys.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.entity.ChatRoomMember;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.service.result.ChatRoomResult;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomMemberRepository chatRoomMemberRepository;
  private final UserRepository userRepository;

  @Transactional
  public ChatRoomResult createOrGetChatRoom(
      Long userId,
      Long participantUserId
  ) {

    if (userId.equals(participantUserId)) {
      throw new BaseException(ChatErrorCode.CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
    }

    User user = findActiveUser(userId);
    User participant = findActiveUser(participantUserId);

    ChatRoom chatRoom = chatRoomMemberRepository.findDirectChatRoom(userId, participantUserId)
        .orElseGet(() -> createChatRoom(user, participant));

    return new ChatRoomResult(chatRoom, participant);
  }

  private User findActiveUser(Long userId) {
    return userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
  }

  private ChatRoom createChatRoom(
      User user,
      User participant
  ) {

    ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create());

    chatRoomMemberRepository.save(new ChatRoomMember(chatRoom, user));
    chatRoomMemberRepository.save(new ChatRoomMember(chatRoom, participant));

    return chatRoom;
  }
}
