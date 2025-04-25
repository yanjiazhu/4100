package com.demo.sys.entity;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.YearMonth;
import java.sql.Date;
import java.time.LocalDate;

@Converter(autoApply = true)
public class YearMonthConverter implements AttributeConverter<YearMonth, Date> {

    @Override
    public Date convertToDatabaseColumn(YearMonth yearMonth) {
        if (yearMonth == null) {
            return null;
        }
        return Date.valueOf(yearMonth.atDay(1));
    }

    @Override
    public YearMonth convertToEntityAttribute(Date date) {
        if (date == null) {
            return null;
        }
        LocalDate localDate = date.toLocalDate();
        return YearMonth.of(localDate.getYear(), localDate.getMonth());
    }
} 