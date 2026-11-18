package com.healthcare.api.common;

/**
 * 手机号工具：统一归一化（去空格/连字符、剥离 +86）并校验中国大陆号码。
 */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /** 归一化为 11 位号码；非法输入抛出 400。 */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new ApiException(400, "手机号不能为空");
        }
        String phone = raw.replaceAll("[\\s-]", "");
        if (phone.startsWith("+86")) {
            phone = phone.substring(3);
        } else if (phone.matches("86\\d{11}")) {
            phone = phone.substring(2);
        }
        return phone;
    }

    /** 仅接受中国大陆手机号：11 位、1 开头、第二位 3-9。 */
    public static String normalizeAndValidate(String raw) {
        String phone = normalize(raw);
        if (!phone.matches("1[3-9]\\d{9}")) {
            throw new ApiException(400, "请输入正确的 11 位中国大陆手机号");
        }
        return phone;
    }

    /** 展示用脱敏：138 **** 8000。 */
    public static String mask(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 3) + " **** " + phone.substring(7);
    }
}
