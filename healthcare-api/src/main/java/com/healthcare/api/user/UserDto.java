package com.healthcare.api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 账号返回 DTO：隐藏密码等敏感字段。
 */
public record UserDto(Long id, String name, String phone, String email, boolean hasPassword) {

    public static UserDto from(User user) {
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        return new UserDto(user.getId(), user.getName(), user.getPhone(), user.getEmail(), hasPassword);
    }

    public record UpdateRequest(
            @Size(max = 64, message = "昵称不能超过 64 个字符") String name,
            @Email(message = "邮箱格式不正确") @Size(max = 128, message = "邮箱过长") String email,
            @Pattern(
                regexp = "^\\s*(?:\\+?86)?[\\s-]*1[3-9](?:[\\s-]*\\d){9}\\s*$",
                message = "请输入正确的 11 位中国大陆手机号") String phone) {
    }
}
