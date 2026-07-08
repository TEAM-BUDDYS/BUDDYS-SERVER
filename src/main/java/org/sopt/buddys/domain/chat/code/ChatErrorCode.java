package org.sopt.buddys.domain.chat.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {

  CANNOT_CREATE_CHAT_ROOM_WITH_SELF("CHAT-E001", HttpStatus.BAD_REQUEST, "자기 자신과는 채팅방을 생성할 수 없습니다."),
  CHAT_ROOM_NOT_FOUND("CHAT-E002", HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
