package org.sopt.buddys.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.entity.ChatRoomMember;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ChatRoomCommandService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomMemberRepository chatRoomMemberRepository;
  private final UserRepository userRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ChatRoom createDirectChatRoom(
      Long userId,
      Long participantUserId,
      String directChatKey
  ) {

    User user = userRepository.getReferenceById(userId);
    User participant = userRepository.getReferenceById(participantUserId);
    ChatRoom chatRoom = chatRoomRepository.saveAndFlush(ChatRoom.createDirect(directChatKey));

    chatRoomMemberRepository.save(new ChatRoomMember(chatRoom, user));
    chatRoomMemberRepository.save(new ChatRoomMember(chatRoom, participant));

    return chatRoom;
  }
}
