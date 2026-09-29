package org.sopt.buddys.domain.search.service.result;

import org.sopt.buddys.domain.course.service.result.CourseSearchResult;
import org.sopt.buddys.domain.post.service.result.PostListResult;

public record SearchResult(
    CourseSearchResult courses,
    UserSearchResult users,
    PostListResult posts
) {
}
