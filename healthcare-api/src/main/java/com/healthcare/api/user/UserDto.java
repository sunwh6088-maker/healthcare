package com.healthcare.api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 账号返回 DTO：隐藏密码等敏感字段。
 */
public record UserDto(
        Long id,
        String name,
        String phone,
        String email,
        boolean hasPassword,
        String avatarUrl) {

    public static UserDto from(User user) {
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        String avatarUrl = user.isHasAvatar()
                ? "/api/me/avatar?v=" + user.getAvatarVersion()
                : null;
        return new UserDto(
                user.getId(), user.getName(), user.getPhone(), user.getEmail(), hasPassword, avatarUrl);
    }

    /** PATCH /api/me 只允许修改昵称和邮箱；手机号必须走验证码流程。 */
    public record UpdateRequest(
            @Size(max = 64, message = "昵称不能超过 64 个字符") String name,
            @Email(message = "邮箱格式不正确") @Size(max = 128, message = "邮箱过长") String email) {
    }

    /** 修改手机号第 1 步：向新号码发送验证码。 */
    public record PhoneSendCodeRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(
                regexp = "^\\s*(?:\\+?86)?[\\s-]*1[3-9](?:[\\s-]*\\d){9}\\s*$",
                message = "请输入正确的 11 位中国大陆手机号") String phone) {
    }

    /** 修改手机号第 2 步：携带新号码、验证码和当前身份凭证。 */
    public record PhoneUpdateRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(
                regexp = "^\\s*(?:\\+?86)?[\\s-]*1[3-9](?:[\\s-]*\\d){9}\\s*$",
                message = "请输入正确的 11 位中国大陆手机号") String phone,
            @NotBlank(message = "验证码不能为空") String code,
            @Size(max = 64, message = "当前密码过长") String currentPassword,
            String currentCode) {
    }

    /** 注销等敏感操作的二次凭证。 */
    public record StepUpRequest(
            @Size(max = 64, message = "密码过长") String password,
            String code) {
    }

    /** 开发模式下附带 devCode 便于联调；生产环境 devCode 为 null。 */
    public record PhoneCodeResponse(String message, String devCode) {
    }
}
