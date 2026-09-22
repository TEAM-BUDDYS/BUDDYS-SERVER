package org.sopt.buddys.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.entity.ChatUserReport;
import org.sopt.buddys.domain.chat.repository.ChatUserReportRepository;
import org.sopt.buddys.domain.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ChatUserReportCommandService {

  private final ChatUserReportRepository chatUserReportRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ChatUserReport save(
      ChatRoom chatRoom,
      User reporter,
      User reported,
      String reason
  ) {

    return chatUserReportRepository.save(new ChatUserReport(chatRoom, reporter, reported, reason));
  }
}
