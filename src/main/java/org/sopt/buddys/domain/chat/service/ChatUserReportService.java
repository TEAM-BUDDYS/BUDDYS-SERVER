package org.sopt.buddys.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.entity.ChatUserReport;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatUserReportService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomMemberRepository chatRoomMemberRepository;
  private final ChatUserReportCommandService chatUserReportCommandService;
  private final UserRepository userRepository;
  private final ChatReportMailSender chatReportMailSender;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void reportChatPartner(Long userId, Long chatRoomId, String reason) {
    User reporter = getActiveUser(userId);
    ChatRoomMemberRepository.ChatRoomDetailProjection chatRoomDetail =
        chatRoomMemberRepository.findChatRoomDetailByIdAndUserId(chatRoomId, userId)
            .orElseThrow(() -> chatRoomAccessException(chatRoomId));

    ChatRoom chatRoom = chatRoomDetail.getChatRoom();
    User reported = chatRoomDetail.getParticipant();

    ChatUserReport report = chatUserReportCommandService.save(chatRoom, reporter, reported, reason);

    sendReportMail(report);
  }

  private void sendReportMail(ChatUserReport report) {
    try {
      chatReportMailSender.send(report);
    } catch (BaseException e) {
      log.error(
          "[ChatReportMailSendFailed] reportId={}, chatRoomId={}, reporterId={}, reportedId={}, code={}",
          report.getId(),
          report.getChatRoom().getId(),
          report.getReporter().getId(),
          report.getReported().getId(),
          e.getErrorCode().getCode(),
          e
      );
    }
  }

  private User getActiveUser(Long userId) {
    return userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
  }

  private BaseException chatRoomAccessException(Long chatRoomId) {
    if (!chatRoomRepository.existsById(chatRoomId)) {
      return new BaseException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
    }
    return new BaseException(GlobalErrorCode.FORBIDDEN);
  }
}
