package org.sopt.buddys.domain.chat.repository;

import org.sopt.buddys.domain.chat.entity.ChatUserReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatUserReportRepository extends JpaRepository<ChatUserReport, Long> {
}
