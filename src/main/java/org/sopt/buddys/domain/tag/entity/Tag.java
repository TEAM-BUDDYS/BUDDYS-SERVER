package org.sopt.buddys.domain.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "tag",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tag_type_name", columnNames = {"tag_type", "name"})
    }
)
public class Tag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 30)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "tag_type", nullable = false, length = 20)
  private TagType tagType;
}