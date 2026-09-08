package org.sopt.buddys.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatRoomMemberId;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.repository.ChatUserBlockRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatUserBlockService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomMemberRepository chatRoomMemberRepository;
  private final ChatUserBlockRepository chatUserBlockRepository;

  @Transactional
  public void blockChatPartner(Long userId, Long chatRoomId) {
    Long partnerId = getChatPartnerId(userId, chatRoomId);
    chatUserBlockRepository.insertOrKeep(userId, partnerId);
  }

  private Long getChatPartnerId(Long userId, Long chatRoomId) {
    if (!chatRoomMemberRepository.existsById(new ChatRoomMemberId(chatRoomId, userId))) {
      throw chatRoomAccessException(chatRoomId);
    }

    return chatRoomMemberRepository.findOtherMemberUserId(chatRoomId, userId)
        .orElseThrow(() -> chatRoomAccessException(chatRoomId));
  }

  private BaseException chatRoomAccessException(Long chatRoomId) {
    if (!chatRoomRepository.existsById(chatRoomId)) {
      return new BaseException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
    }
    return new BaseException(GlobalErrorCode.FORBIDDEN);
  }
}
