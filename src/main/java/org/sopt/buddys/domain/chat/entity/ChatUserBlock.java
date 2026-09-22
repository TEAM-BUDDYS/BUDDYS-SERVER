package org.sopt.buddys.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.buddys.domain.chat.util.ChatTimeConverter;
import org.sopt.buddys.domain.user.entity.User;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_user_block")
public class ChatUserBlock {

  @EmbeddedId
  private ChatUserBlockId id;

  @MapsId("blockerId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "blocker_id", nullable = false)
  private User blocker;

  @MapsId("blockedId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "blocked_id", nullable = false)
  private User blocked;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public ChatUserBlock(User blocker, User blocked) {
    this.blocker = blocker;
    this.blocked = blocked;
    this.id = new ChatUserBlockId(blocker.getId(), blocked.getId());
  }

  @PrePersist
  private void prePersist() {
    if (createdAt == null) {
      createdAt = ChatTimeConverter.now();
    }
  }
}
