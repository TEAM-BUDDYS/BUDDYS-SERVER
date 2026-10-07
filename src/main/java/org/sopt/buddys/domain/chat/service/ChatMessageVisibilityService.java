package org.sopt.buddys.domain.chat.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.repository.ChatUserBlockRepository;
import org.sopt.buddys.domain.chat.repository.ChatUserReportRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ChatMessageVisibilityService {

  private final ChatUserBlockRepository chatUserBlockRepository;
  private final ChatUserReportRepository chatUserReportRepository;

  public LocalDateTime findPartnerMessagesHiddenAfter(Long userId, Long partnerId) {
    if (partnerId == null) {
      return null;
    }

    return Stream.of(
            chatUserBlockRepository.findBlockedAt(userId, partnerId),
            chatUserReportRepository.findFirstReportedAt(userId, partnerId)
        )
        .flatMap(Optional::stream)
        .min(LocalDateTime::compareTo)
        .orElse(null);
  }
}
