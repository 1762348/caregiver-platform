package com.youhu.caregiver.common.util;

import com.youhu.caregiver.common.domain.Gender;
import com.youhu.caregiver.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 18 位身份证工具：校验位、出生日期与性别解析。
 */
public final class IdCardUtils {

    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
    private static final DateTimeFormatter BIRTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private IdCardUtils() {
    }

    public static void validate(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            throw new BusinessException(400, "身份证号必须为 18 位");
        }
        char checkCode = idCard.charAt(17);
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = idCard.charAt(i);
            if (c < '0' || c > '9') {
                throw new BusinessException(400, "身份证号格式不正确");
            }
            sum += (c - '0') * WEIGHTS[i];
        }
        char expected = CHECK_CODES[sum % 11];
        if (Character.toUpperCase(checkCode) != expected) {
            throw new BusinessException(400, "身份证号校验位不正确");
        }
        try {
            LocalDate birth = birthDate(idCard);
            if (birth.isAfter(LocalDate.now())) {
                throw new BusinessException(400, "身份证出生日期不合法");
            }
        } catch (DateTimeParseException ex) {
            throw new BusinessException(400, "身份证出生日期不合法");
        }
    }

    public static LocalDate birthDate(String idCard) {
        return LocalDate.parse(idCard.substring(6, 14), BIRTH_FORMAT);
    }

    public static Gender gender(String idCard) {
        int genderDigit = idCard.charAt(16) - '0';
        return genderDigit % 2 == 0 ? Gender.FEMALE : Gender.MALE;
    }
}
