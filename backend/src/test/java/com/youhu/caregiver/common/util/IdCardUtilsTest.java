package com.youhu.caregiver.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.youhu.caregiver.common.domain.Gender;
import com.youhu.caregiver.common.exception.BusinessException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IdCardUtilsTest {

    private static final String VALID_ID = "50010119900101123X";

    @Test
    void parsesBirthAndGender() {
        assertThat(IdCardUtils.birthDate(VALID_ID)).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(IdCardUtils.gender(VALID_ID)).isEqualTo(Gender.MALE);
        assertThat(IdCardUtils.gender("50010119900101124X")).isEqualTo(Gender.FEMALE);
    }

    @Test
    void rejectsWrongLength() {
        assertThatThrownBy(() -> IdCardUtils.validate("123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("18 位");
    }

    @Test
    void rejectsWrongCheckDigit() {
        assertThatThrownBy(() -> IdCardUtils.validate("500101199001011234"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("校验位");
    }

    @Test
    void acceptsValidId() {
        IdCardUtils.validate(VALID_ID);
    }
}
