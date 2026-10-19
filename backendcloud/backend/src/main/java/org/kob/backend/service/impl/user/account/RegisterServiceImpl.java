package org.kob.backend.service.impl.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.kob.backend.mapper.UserMapper;
import org.kob.backend.pojo.User;
import org.kob.backend.service.user.account.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RegisterServiceImpl implements RegisterService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Map<String, String> register(String username, String password, String confirmedPassword) {
        Map<String, String> map = new HashMap<>();
        if (username == null || username.isBlank()) {
            map.put("error_message", "Username cannot be empty");
            return map;
        }
        if (password == null || confirmedPassword == null || password.isEmpty() || confirmedPassword.isEmpty()) {
            map.put("error_message", "Password cannot be empty");
            return map;
        }
        username = username.trim();
        if (username.length() > 100) {
            map.put("error_message", "Username cannot exceed 100 characters");
            return map;
        }
        if (password.length() > 100 || confirmedPassword.length() > 100) {
            map.put("error_message", "Password cannot exceed 100 characters");
            return map;
        }
        if (!password.equals(confirmedPassword)) {
            map.put("error_message", "Passwords do not match");
            return map;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        Long count = userMapper.selectCount(queryWrapper); // count rows instead of loading them
        if (count > 0) {
            map.put("error_message", "Username is already taken");
            return map;
        }
        String encodePassword = passwordEncoder.encode(password);
        String photo = "https://api.dicebear.com/10.x/bottts/svg?seed=" + username;
        User user = new User(null, username, encodePassword, photo, 1500, 0, 0, 0);
        userMapper.insert(user);
        map.put("error_message", "success");
        return map;
    }
}
