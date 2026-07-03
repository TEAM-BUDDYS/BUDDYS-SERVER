package org.sopt.buddys.domain.chat.repository;

import java.util.Optional;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  Optional<ChatRoom> findByDirectChatKey(String directChatKey);
}
