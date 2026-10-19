package org.kob.backend.service.impl.user.bot;

import org.kob.backend.mapper.BotMapper;
import org.kob.backend.pojo.Bot;
import org.kob.backend.pojo.User;
import org.kob.backend.service.impl.utils.BotCodeChecker;
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
    @Autowired
    private BotCodeChecker botCodeChecker;

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
            map.put("error_message", "Bot does not exist or was already deleted");
            return map;
        }

        if (!bot.getUserId().equals(user.getId())) {
            map.put("error_message", "You do not have permission to edit this bot.");
            return map;
        }

        String title = data.get("title");
        String description = data.get("description");
        String content = data.get("content");
        String language = data.get("language");

        if (title == null || title.isBlank()) {
            map.put("error_message", "Bot name cannot be empty");
            return map;
        }
        if (title.length() > 100) {
            map.put("error_message", "Bot name cannot exceed 100 characters");
            return map;
        }
        if (description == null || description.isBlank()) {
            description = "No description provided.";
        }
        if (description.length() > 300) {
            map.put("error_message", "Bot description cannot exceed 300 characters");
            return map;
        }
        if (content == null || content.isBlank()) {
            map.put("error_message", "Bot code cannot be empty");
            return map;
        }
        if (content.length() > 10000) {
            map.put("error_message", "Bot code cannot exceed 10,000 characters");
            return map;
        }
        if (language == null || !BotCodeChecker.LANGUAGES.contains(language)) {
            map.put("error_message", "Unsupported language");
            return map;
        }
        String compileError = botCodeChecker.check(language, content);
        if (compileError != null) {
            map.put("error_message", compileError);
            return map;
        }
        Bot new_bot = new Bot(
                bot.getId(),
                bot.getUserId(),
                title,
                description,
                content,
                language,
                bot.getCreateTime(),
                new Date());

        botMapper.updateById(new_bot);
        map.put("error_message", "success");
        return map;

    }
}
