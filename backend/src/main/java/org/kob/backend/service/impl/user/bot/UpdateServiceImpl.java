package org.kob.backend.service.impl.user.bot;

import org.kob.backend.mapper.BotMapper;
import org.kob.backend.pojo.Bot;
import org.kob.backend.pojo.User;
import org.kob.backend.service.impl.utils.UserDetailsImpl;
import org.kob.backend.service.user.bot.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class UpdateServiceImpl implements UpdateService {
    @Autowired
    private BotMapper botMapper;

    @Override
    public Map<String, String> updateBot(Map<String, String> data) {
        UsernamePasswordAuthenticationToken authenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticationToken.getPrincipal();
        User user = loginUser.getUser();

        int bot_id = Integer.parseInt(data.get("bot_id"));
        Bot bot = botMapper.selectById(bot_id);
        Map<String, String> map = new HashMap<>();

        if (bot == null) {
            map.put("error_message", "Bot不存在或已被删除");
            return map;
        }

        if (!bot.getUserId().equals(user.getId())) {
            map.put("error_message", "没有权限修改该Bot！");
            return map;
        }

        String title = data.get("title");
        String description = data.get("description");
        String content = data.get("content");

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
        Bot new_bot = new Bot(
                bot.getId(),
                bot.getUserId(),
                title,
                description,
                content,
                bot.getRating(),
                bot.getCreateTime(),
                new Date());

        botMapper.updateById(new_bot);
        map.put("error_message", "success");
        return map;

    }
}
