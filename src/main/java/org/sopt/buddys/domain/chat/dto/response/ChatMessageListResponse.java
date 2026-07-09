package org.sopt.buddys.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import org.sopt.buddys.domain.chat.entity.ChatMessage;
import org.sopt.buddys.domain.chat.service.result.ChatMessageListResult;
import org.sopt.buddys.domain.chat.service.result.ChatMessageListResult.ChatMessageResult;
import org.sopt.buddys.domain.chat.util.ChatTimeConverter;

public record ChatMessageListResponse(
    @Schema(description = "메시지 목록")
    List<ChatMessageResponse> messages,

    @Schema(description = "다음 페이지 커서 메시지 전송 시각. UTC 기준입니다.", example = "2026-07-07T14:30:00Z")
    OffsetDateTime nextCursorSentAt,

    @Schema(description = "다음 페이지 커서 메시지 ID", example = "101")
    Long nextCursorMessageId,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext
) {

  public ChatMessageListResponse {
    messages = List.copyOf(messages);
  }

  public static ChatMessageListResponse from(ChatMessageListResult result) {
    List<ChatMessageResponse> messages = result.messages()
        .stream()
        .map(ChatMessageResponse::from)
        .toList();

    return new ChatMessageListResponse(
        messages,
        ChatTimeConverter.toCommonTime(result.nextCursorSentAt()),
        result.nextCursorMessageId(),
        result.hasNext()
    );
  }

  public record ChatMessageResponse(
      @Schema(description = "메시지 ID", example = "101")
      Long messageId,

      @Schema(description = "보낸 사용자 정보")
      ChatParticipantResponse sender,

      @Schema(description = "메시지 내용", example = "안녕하세요!")
      String content,

      @Schema(description = "메시지 전송 시각. UTC 기준입니다.", example = "2026-07-07T14:30:00Z")
      OffsetDateTime sentAt,

      @Schema(description = "내가 보낸 메시지 여부", example = "false")
      boolean mine,

      @JsonProperty("isRead")
      @Schema(description = "메시지를 읽어야 하는 사용자가 읽은 여부", example = "true")
      boolean read
  ) {

    private static ChatMessageResponse from(ChatMessageResult result) {
      ChatMessage message = result.message();

      return new ChatMessageResponse(
          message.getId(),
          ChatParticipantResponse.from(message.getSender()),
          message.getMessage(),
          ChatTimeConverter.toCommonTime(message.getCreatedAt()),
          result.mine(),
          result.read()
      );
    }
  }
}
