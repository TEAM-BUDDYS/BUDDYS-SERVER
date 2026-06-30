package org.sopt.buddys.domain.post.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.buddys.domain.tag.entity.Tag;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_tag")
public class PostTag {

  @EmbeddedId
  private PostTagId id;

  @MapsId("postId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @MapsId("tagId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tag_id", nullable = false)
  private Tag tag;
}