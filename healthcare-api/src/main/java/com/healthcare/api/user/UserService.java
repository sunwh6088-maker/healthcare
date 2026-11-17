package com.healthcare.api.user;

import com.healthcare.api.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号业务：手机号、邮箱、昵称和密码更新。
 */
@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
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
            String phone = request.phone().replaceAll("\\s", "");
            if (!phone.matches("\\d{7,15}")) {
                throw new ApiException(400, "手机号格式不正确");
            }
            if (!phone.equals(user.getPhone()) && users.existsByPhoneAndIdNot(phone, userId)) {
                throw new ApiException(400, "手机号已被使用");
            }
            user.setPhone(phone);
        }

        return UserDto.from(users.save(user));
    }
}
