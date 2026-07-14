package org.sopt.buddys.domain.chat.repository;

import java.util.Optional;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository.ChatRoomListProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ChatRoomMemberRepositoryCustom {

  Slice<ChatRoomListProjection> findChatRoomListByUserId(
      Long userId,
      Pageable pageable
  );

  Optional<ChatRoomListProjection> findChatRoomListItemByUserIdAndChatRoomId(
      Long userId,
      Long chatRoomId
  );
}
