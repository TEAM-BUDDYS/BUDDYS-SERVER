package org.sopt.buddys.domain.chat.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  Optional<ChatRoom> findByDirectChatKey(String directChatKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from ChatRoom c where c.id = :chatRoomId")
  Optional<ChatRoom> findByIdForUpdate(@Param("chatRoomId") Long chatRoomId);
}
