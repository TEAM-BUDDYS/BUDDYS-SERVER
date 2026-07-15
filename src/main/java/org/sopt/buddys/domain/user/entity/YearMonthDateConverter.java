package org.sopt.buddys.domain.user.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.YearMonth;

@Converter(autoApply = false)
public class YearMonthDateConverter implements AttributeConverter<YearMonth, LocalDate> {

  @Override
  public LocalDate convertToDatabaseColumn(YearMonth attribute) {
    return attribute == null ? null : attribute.atDay(1);
  }

  @Override
  public YearMonth convertToEntityAttribute(LocalDate dbData) {
    return dbData == null ? null : YearMonth.from(dbData);
  }
}