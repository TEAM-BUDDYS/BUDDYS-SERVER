package org.sopt.buddys.domain.verification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.global.common.entity.BaseEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "exchange_verification")
public class ExchangeVerification extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "document_key", nullable = false, length = 512, unique = true)
  private String documentKey;

  @Column(name = "original_file_name", nullable = false, length = 255)
  private String originalFileName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ExchangeVerificationStatus status;

  @Column(name = "rejection_reason", length = 500)
  private String rejectionReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by")
  private User reviewedBy;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  public ExchangeVerification(
      User user,
      String documentKey,
      String originalFileName,
      String contentType,
      Long fileSize
  ) {
    this.user = user;
    this.documentKey = documentKey;
    this.originalFileName = originalFileName;
    this.contentType = contentType;
    this.fileSize = fileSize;
    this.status = ExchangeVerificationStatus.PENDING;
  }

  public boolean isPending() {
    return status == ExchangeVerificationStatus.PENDING;
  }

  public void approve(User reviewer) {
    validatePending();
    this.status = ExchangeVerificationStatus.APPROVED;
    this.reviewedBy = reviewer;
    this.reviewedAt = LocalDateTime.now();
    this.rejectionReason = null;
  }

  public void reject(User reviewer, String rejectionReason) {
    validatePending();
    this.status = ExchangeVerificationStatus.REJECTED;
    this.reviewedBy = reviewer;
    this.reviewedAt = LocalDateTime.now();
    this.rejectionReason = rejectionReason;
  }

  private void validatePending() {
    if (!isPending()) {
      throw new IllegalStateException("Only pending exchange verification can be reviewed");
    }
  }
}
