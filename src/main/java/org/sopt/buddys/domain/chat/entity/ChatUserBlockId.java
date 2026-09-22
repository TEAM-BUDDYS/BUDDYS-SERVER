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
public class ChatUserBlockId implements Serializable {

  @Column(name = "blocker_id")
  private Long blockerId;

  @Column(name = "blocked_id")
  private Long blockedId;

  public ChatUserBlockId(Long blockerId, Long blockedId) {
    this.blockerId = blockerId;
    this.blockedId = blockedId;
  }
}
