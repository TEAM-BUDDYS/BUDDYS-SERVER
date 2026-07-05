package org.sopt.buddys.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomListResponse(
    @Schema(description = "채팅방 목록")
    List<ChatRoomListItemResponse> chatRooms,

    @Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0")
    int page,

    @Schema(description = "페이지 크기", example = "10")
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext
) {

  public ChatRoomListResponse {
    chatRooms = List.copyOf(chatRooms);
  }

  public static ChatRoomListResponse of(
      List<ChatRoomListItemResponse> chatRooms,
      int page,
      int size,
      boolean hasNext
  ) {
    return new ChatRoomListResponse(chatRooms, page, size, hasNext);
  }

  public record ChatRoomListItemResponse(
      @Schema(description = "채팅방 ID", example = "1")
      Long chatRoomId,

      @Schema(description = "상대방 사용자 정보")
      ChatParticipantResponse participant,

      @Schema(description = "마지막 메시지", example = "내일 몇 시에 만날까요?")
      String lastMessage,

      @Schema(description = "마지막 메시지 시각", example = "2026-07-05T14:30:00")
      LocalDateTime lastMessageSentAt
  ) {
  }
}
