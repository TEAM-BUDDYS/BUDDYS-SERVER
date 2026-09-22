package org.sopt.buddys.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.entity.ChatUserReport;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository.ChatRoomDetailProjection;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatUserReportServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long PARTNER_ID = 2L;
  private static final Long CHAT_ROOM_ID = 10L;

  @Mock
  private ChatRoomRepository chatRoomRepository;

  @Mock
  private ChatRoomMemberRepository chatRoomMemberRepository;

  @Mock
  private ChatUserReportCommandService chatUserReportCommandService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ChatReportMailSender chatReportMailSender;

  @Mock
  private ChatRoomDetailProjection chatRoomDetailProjection;

  private ChatUserReportService chatUserReportService;

  @BeforeEach
  void setUp() {
    chatUserReportService = new ChatUserReportService(
        chatRoomRepository,
        chatRoomMemberRepository,
        chatUserReportCommandService,
        userRepository,
        chatReportMailSender
    );
  }

  @DisplayName("사유 없이 신고하면 신고 내역이 저장되고 운영 메일이 발송된다")
  @Test
  void reportChatPartner_withoutReason_savesReportAndSendsMail() {
    // given
    User reporter = createUser(USER_ID, "신고자");
    User reported = createUser(PARTNER_ID, "신고대상");
    ChatRoom chatRoom = createChatRoom(CHAT_ROOM_ID);
    ChatUserReport report = createReport(chatRoom, reporter, reported, null);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(reporter));
    given(chatRoomMemberRepository.findChatRoomDetailByIdAndUserId(CHAT_ROOM_ID, USER_ID))
        .willReturn(Optional.of(chatRoomDetailProjection));
    given(chatRoomDetailProjection.getChatRoom()).willReturn(chatRoom);
    given(chatRoomDetailProjection.getParticipant()).willReturn(reported);
    given(chatUserReportCommandService.save(chatRoom, reporter, reported, null)).willReturn(report);

    // when
    chatUserReportService.reportChatPartner(USER_ID, CHAT_ROOM_ID, null);

    // then
    then(chatUserReportCommandService).should().save(chatRoom, reporter, reported, null);
    then(chatReportMailSender).should().send(report);
  }

  @DisplayName("사유를 입력해서 신고하면 그 사유가 그대로 커맨드 서비스에 전달된다")
  @Test
  void reportChatPartner_withReason_passesReasonThrough() {
    // given
    User reporter = createUser(USER_ID, "신고자");
    User reported = createUser(PARTNER_ID, "신고대상");
    ChatRoom chatRoom = createChatRoom(CHAT_ROOM_ID);
    String reason = "부적절한 언행";
    ChatUserReport report = createReport(chatRoom, reporter, reported, reason);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(reporter));
    given(chatRoomMemberRepository.findChatRoomDetailByIdAndUserId(CHAT_ROOM_ID, USER_ID))
        .willReturn(Optional.of(chatRoomDetailProjection));
    given(chatRoomDetailProjection.getChatRoom()).willReturn(chatRoom);
    given(chatRoomDetailProjection.getParticipant()).willReturn(reported);
    given(chatUserReportCommandService.save(chatRoom, reporter, reported, reason)).willReturn(report);

    // when
    chatUserReportService.reportChatPartner(USER_ID, CHAT_ROOM_ID, reason);

    // then
    then(chatUserReportCommandService).should().save(chatRoom, reporter, reported, reason);
  }

  @DisplayName("메일 발송이 실패해도 이미 커밋된 신고 기록은 유지되고 예외가 전파되지 않는다")
  @Test
  void reportChatPartner_mailSendFails_reportStillSavedAndNoExceptionPropagates() {
    // given
    User reporter = createUser(USER_ID, "신고자");
    User reported = createUser(PARTNER_ID, "신고대상");
    ChatRoom chatRoom = createChatRoom(CHAT_ROOM_ID);
    ChatUserReport report = createReport(chatRoom, reporter, reported, null);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(reporter));
    given(chatRoomMemberRepository.findChatRoomDetailByIdAndUserId(CHAT_ROOM_ID, USER_ID))
        .willReturn(Optional.of(chatRoomDetailProjection));
    given(chatRoomDetailProjection.getChatRoom()).willReturn(chatRoom);
    given(chatRoomDetailProjection.getParticipant()).willReturn(reported);
    given(chatUserReportCommandService.save(chatRoom, reporter, reported, null)).willReturn(report);
    willThrow(new BaseException(ChatErrorCode.REPORT_MAIL_SEND_FAILED))
        .given(chatReportMailSender).send(report);

    // when, then: 커맨드 서비스의 save()는 이미 별도 트랜잭션으로 커밋되었으므로,
    // 이후 메일 발송이 실패해도 그 실패가 신고 접수 자체를 취소시키지 않는다.
    assertThatCode(() -> chatUserReportService.reportChatPartner(USER_ID, CHAT_ROOM_ID, null))
        .doesNotThrowAnyException();
    then(chatUserReportCommandService).should().save(chatRoom, reporter, reported, null);
  }

  @DisplayName("탈퇴한 사용자가 신고하면 USER_NOT_FOUND 예외가 발생한다")
  @Test
  void reportChatPartner_deletedUser_throwsUserNotFound() {
    // given
    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> chatUserReportService.reportChatPartner(USER_ID, CHAT_ROOM_ID, null))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    then(chatReportMailSender).should(never()).send(any());
  }

  @DisplayName("존재하지 않는 채팅방을 신고하면 CHAT_ROOM_NOT_FOUND 예외가 발생한다")
  @Test
  void reportChatPartner_notFoundChatRoom_throwsChatRoomNotFound() {
    // given
    User reporter = createUser(USER_ID, "신고자");
    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(reporter));
    given(chatRoomMemberRepository.findChatRoomDetailByIdAndUserId(CHAT_ROOM_ID, USER_ID))
        .willReturn(Optional.empty());
    given(chatRoomRepository.existsById(CHAT_ROOM_ID)).willReturn(false);

    // when, then
    assertThatThrownBy(() -> chatUserReportService.reportChatPartner(USER_ID, CHAT_ROOM_ID, null))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
  }

  @DisplayName("채팅방 멤버가 아닌 사용자가 신고하면 FORBIDDEN 예외가 발생한다")
  @Test
  void reportChatPartner_notMember_throwsForbidden() {
    // given
    User reporter = createUser(USER_ID, "신고자");
    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(reporter));
    given(chatRoomMemberRepository.findChatRoomDetailByIdAndUserId(CHAT_ROOM_ID, USER_ID))
        .willReturn(Optional.empty());
    given(chatRoomRepository.existsById(CHAT_ROOM_ID)).willReturn(true);

    // when, then
    assertThatThrownBy(() -> chatUserReportService.reportChatPartner(USER_ID, CHAT_ROOM_ID, null))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(GlobalErrorCode.FORBIDDEN);
  }

  private User createUser(Long id, String nickname) {
    return User.builder()
        .id(id)
        .email(nickname + "@test.com")
        .provider(AuthProvider.KAKAO)
        .providerId("provider-" + id)
        .nickname(nickname)
        .build();
  }

  private ChatRoom createChatRoom(Long id) {
    ChatRoom chatRoom = ChatRoom.createDirect("direct-chat-key");
    ReflectionTestUtils.setField(chatRoom, "id", id);
    return chatRoom;
  }

  private ChatUserReport createReport(ChatRoom chatRoom, User reporter, User reported, String reason) {
    ChatUserReport report = new ChatUserReport(chatRoom, reporter, reported, reason);
    ReflectionTestUtils.setField(report, "id", 100L);
    return report;
  }
}
