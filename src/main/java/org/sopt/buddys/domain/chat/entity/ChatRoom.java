package org.sopt.buddys.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "chat_room",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_chat_room_direct_chat_key", columnNames = "direct_chat_key")
    }
)
public class ChatRoom {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "direct_chat_key", length = 50)
  private String directChatKey;

  public static ChatRoom createDirect(String directChatKey) {
    ChatRoom chatRoom = new ChatRoom();
    chatRoom.directChatKey = directChatKey;
    return chatRoom;
  }
}
