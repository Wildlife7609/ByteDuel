package org.kob.backend.service.impl.user.bot;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.kob.backend.mapper.BotMapper;
import org.kob.backend.pojo.Bot;
import org.kob.backend.pojo.User;
import org.kob.backend.service.impl.utils.BotCodeChecker;
import org.kob.backend.service.impl.utils.UserDetailsImpl;
import org.kob.backend.service.user.bot.AddService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AddServiceImpl implements AddService {
    @Autowired
    private BotMapper botMapper;
    @Autowired
    private BotCodeChecker botCodeChecker;

    @Override
    public Map<String, String> addBot(Map<String, String> data) {
        UsernamePasswordAuthenticationToken authenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticationToken.getPrincipal();
        User user = loginUser.getUser();
        String title = data.get("title");
        String description = data.get("description");
        String content = data.get("content");

        Map<String, String> map = new HashMap<>();

        if (title == null || title.isBlank()) {
            map.put("error_message", "标题不能为空");
            return map;
        }
        if (title.length() > 100) {
            map.put("error_message", "标题长度不能大于100");
            return map;
        }
        if (description == null || description.isBlank()) {
            description = "这个用户很懒，什么也没留下~";
        }
        if (description.length() > 300) {
            map.put("error_message", "描述长度不能大于300");
            return map;
        }
        if (content == null || content.isBlank()) {
            map.put("error_message", "内容不能为空");
            return map;
        }
        if (content.length() > 10000) {
            map.put("error_message", "内容长度不能大于10000");
            return map;
        }

        QueryWrapper<Bot> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId());
        if (botMapper.selectCount(queryWrapper) >= 10) {
            map.put("error_message", "每个用户最多只能创建10个Bot！");
            return map;
        }
        // 编译比较耗时，放在其他检查都通过之后
        String compileError = botCodeChecker.check(content);
        if (compileError != null) {
            map.put("error_message", compileError);
            return map;
        }

        Date now = new Date();
        Bot bot = new Bot(
                null,
                user.getId(),
                title,
                description,
                content,
                now,
                now);
        botMapper.insert(bot);
        map.put("error_message", "success");
        return map;
    }
}