package org.sopt.buddys.domain.chat.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatMessage;
import org.sopt.buddys.domain.chat.entity.ChatRoomMemberId;
import org.sopt.buddys.domain.chat.repository.ChatMessageRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.domain.chat.repository.ChatRoomRepository;
import org.sopt.buddys.domain.chat.service.result.ChatMessageListResult;
import org.sopt.buddys.domain.chat.service.result.ChatMessageListResult.ChatMessageResult;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;

    public ChatMessageListResult getMessages(
            Long userId,
            Long chatRoomId,
            LocalDateTime cursorSentAt,
            Long cursorMessageId,
            int size
    ) {

        validateUserExists(userId);
        validatePageRequest(size);
        validateCursor(cursorSentAt, cursorMessageId);
        validateChatRoomAccess(userId, chatRoomId);

        Pageable pageable = PageRequest.of(0, size + 1);
        List<ChatMessage> messages = chatMessageRepository.findMessagesByChatRoomId(
                chatRoomId,
                cursorSentAt,
                cursorMessageId,
                pageable
        );
        boolean hasNext = messages.size() > size;
        List<ChatMessage> pageMessages = hasNext ? messages.subList(0, size) : messages;
        Long myLastReadMessageId = chatRoomMemberRepository
                .findMyLastReadMessageId(chatRoomId, userId)
                .orElse(null);
        Long participantLastReadMessageId = chatRoomMemberRepository
                .findParticipantLastReadMessageId(chatRoomId, userId)
                .orElse(null);
        List<ChatMessageResult> messageResults = pageMessages.stream()
                .map(message -> new ChatMessageResult(
                        message,
                        message.getSender().getId().equals(userId),
                        isRead(
                                message,
                                userId,
                                myLastReadMessageId,
                                participantLastReadMessageId
                        )
                ))
                .toList();

        ChatMessage nextCursorMessage = pageMessages.isEmpty()
                ? null
                : pageMessages.get(pageMessages.size() - 1);

        return new ChatMessageListResult(
                messageResults,
                nextCursorMessage == null ? null : nextCursorMessage.getCreatedAt(),
                nextCursorMessage == null ? null : nextCursorMessage.getId(),
                hasNext
        );
    }

    private boolean isRead(
            ChatMessage message,
            Long userId,
            Long myLastReadMessageId,
            Long participantLastReadMessageId
    ) {
        Long lastReadMessageId = message.getSender().getId().equals(userId)
                ? participantLastReadMessageId
                : myLastReadMessageId;

        return lastReadMessageId != null && message.getId() <= lastReadMessageId;
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new BaseException(UserErrorCode.USER_NOT_FOUND);
        }
    }

    private void validatePageRequest(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
        }
    }

    private void validateCursor(
            LocalDateTime cursorSentAt,
            Long cursorMessageId
    ) {
        if ((cursorSentAt == null) != (cursorMessageId == null)) {
            throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
        }
    }

    private void validateChatRoomAccess(
            Long userId,
            Long chatRoomId
    ) {
        ChatRoomMemberId chatRoomMemberId = new ChatRoomMemberId(chatRoomId, userId);

        if (chatRoomMemberRepository.existsById(chatRoomMemberId)) {
            return;
        }

        if (!chatRoomRepository.existsById(chatRoomId)) {
            throw new BaseException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        throw new BaseException(GlobalErrorCode.FORBIDDEN);
    }
}
