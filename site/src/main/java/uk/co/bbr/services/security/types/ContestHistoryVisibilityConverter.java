package uk.co.bbr.services.security.types;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class ContestHistoryVisibilityConverter implements AttributeConverter<ContestHistoryVisibility, String> {
    @Override
    public String convertToDatabaseColumn(ContestHistoryVisibility category) {
        if (category == null) {
            return null;
        }
        return category.getCode();
    }

    @Override
    public ContestHistoryVisibility convertToEntityAttribute(String code) {
        return ContestHistoryVisibility.fromCode(code);
    }
}
