package org.sopt.buddys.domain.course.service.command;

import java.math.BigDecimal;

public record CoursePlaceCommand(
    String googlePlaceId,
    String name,
    String category,
    BigDecimal latitude,
    BigDecimal longitude,
    Short orderNo,
    String memo,
    BigDecimal cost
) {
}
