package org.sopt.buddys.domain.search.service.result;

import org.sopt.buddys.domain.course.service.result.CourseListResult;
import org.sopt.buddys.domain.post.service.result.PostListResult;

public record SearchResult(
    CourseListResult courses,
    UserSearchResult users,
    PostListResult posts
) {
}
