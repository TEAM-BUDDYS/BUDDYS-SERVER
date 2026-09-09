package org.sopt.buddys.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "chat_user_report")
public class ChatUserReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "chat_room_id", nullable = false)
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reporter_id", nullable = false)
  private User reporter;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reported_id", nullable = false)
  private User reported;

  @Column(length = 500)
  private String reason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public ChatUserReport(ChatRoom chatRoom, User reporter, User reported, String reason) {
    this.chatRoom = chatRoom;
    this.reporter = reporter;
    this.reported = reported;
    this.reason = reason;
  }

  @PrePersist
  private void prePersist() {
    if (createdAt == null) {
      createdAt = ChatTimeConverter.now();
    }
  }
}
