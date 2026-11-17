package com.healthcare.api.user;

import com.healthcare.api.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号业务：手机号、邮箱、昵称和密码更新。
 */
@Service
public class UserService {

    private final UserRepository users;
    private final JdbcTemplate jdbc;

    public UserService(UserRepository users, JdbcTemplate jdbc) {
        this.users = users;
        this.jdbc = jdbc;
    }

    public User get(Long userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(404, "用户不存在"));
    }

    @Transactional
    public UserDto update(Long userId, UserDto.UpdateRequest request) {
        User user = get(userId);

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.email() != null) {
            user.setEmail(request.email().trim());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            String phone = request.phone().replaceAll("[\\s-]", "");
            if (phone.startsWith("+86")) {
                phone = phone.substring(3);
            } else if (phone.matches("86\\d{11}")) {
                phone = phone.substring(2);
            }
            if (!phone.matches("1[3-9]\\d{9}")) {
                throw new ApiException(400, "请输入正确的 11 位中国大陆手机号");
            }
            if (!phone.equals(user.getPhone()) && users.existsByPhoneAndIdNot(phone, userId)) {
                throw new ApiException(400, "手机号已被使用");
            }
            user.setPhone(phone);
        }

        return UserDto.from(users.save(user));
    }

    /**
     * 注销账号：删除该用户的登录凭证、健康数据、闹钟、食物和运动记录。
     */
    @Transactional
    public void deleteAccount(Long userId) {
        get(userId);
        jdbc.update("DELETE FROM auth_tokens WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM alarms WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM food_logs WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM exercise_logs WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM health_states WHERE user_id = ?", userId);
        users.deleteById(userId);
    }
}
