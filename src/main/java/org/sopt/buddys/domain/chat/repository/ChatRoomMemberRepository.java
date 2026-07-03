package org.sopt.buddys.domain.chat.repository;

import org.sopt.buddys.domain.chat.entity.ChatRoomMember;
import org.sopt.buddys.domain.chat.entity.ChatRoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {
}
