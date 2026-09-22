package org.sopt.buddys.domain.chat.repository;

import org.sopt.buddys.domain.chat.entity.ChatUserBlock;
import org.sopt.buddys.domain.chat.entity.ChatUserBlockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatUserBlockRepository extends JpaRepository<ChatUserBlock, ChatUserBlockId> {

  @Modifying
  @Query(value = """
      INSERT INTO chat_user_block (blocker_id, blocked_id, created_at)
      VALUES (:blockerId, :blockedId, CURRENT_TIMESTAMP(6))
      ON DUPLICATE KEY UPDATE created_at = created_at
      """, nativeQuery = true)
  int insertOrKeep(
      @Param("blockerId") Long blockerId,
      @Param("blockedId") Long blockedId
  );

  @Query("""
      select case when count(b) > 0 then true else false end
      from ChatUserBlock b
      where (b.blocker.id = :userId1 and b.blocked.id = :userId2)
         or (b.blocker.id = :userId2 and b.blocked.id = :userId1)
      """)
  boolean existsBlockBetween(
      @Param("userId1") Long userId1,
      @Param("userId2") Long userId2
  );
}
