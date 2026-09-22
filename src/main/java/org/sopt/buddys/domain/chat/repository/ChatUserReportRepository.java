package org.sopt.buddys.domain.chat.repository;

import org.sopt.buddys.domain.chat.entity.ChatUserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatUserReportRepository extends JpaRepository<ChatUserReport, Long> {

  @Query("""
      select case when count(r) > 0 then true else false end
      from ChatUserReport r
      where (r.reporter.id = :userId1 and r.reported.id = :userId2)
         or (r.reporter.id = :userId2 and r.reported.id = :userId1)
      """)
  boolean existsReportBetween(
      @Param("userId1") Long userId1,
      @Param("userId2") Long userId2
  );
}
