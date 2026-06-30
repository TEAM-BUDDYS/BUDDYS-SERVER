package org.sopt.buddys.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMemberId implements Serializable {

  @Column(name = "chat_room_id")
  private Long chatRoomId;

  @Column(name = "user_id")
  private Long userId;

  public ChatRoomMemberId(Long chatRoomId, Long userId) {
    this.chatRoomId = chatRoomId;
    this.userId = userId;
  }
}