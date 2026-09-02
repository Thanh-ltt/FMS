package com.FMS.converter;

import com.FMS.enums.ExpenseType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Converter
public class ExpenseTypeListConverter implements AttributeConverter<List<ExpenseType>, String> {
    @Override
    public String convertToDatabaseColumn(List<ExpenseType> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(Enum::name)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    @Override
    public List<ExpenseType> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(ExpenseType::valueOf)
                .distinct()
                .toList();
    }
}
