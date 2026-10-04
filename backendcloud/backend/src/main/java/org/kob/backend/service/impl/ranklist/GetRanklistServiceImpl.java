package org.kob.backend.service.impl.ranklist;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.kob.backend.mapper.UserMapper;
import org.kob.backend.pojo.User;
import org.kob.backend.service.ranklist.GetRanklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GetRanklistServiceImpl implements GetRanklistService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public Map<String, Object> getRanklist(Integer page) {
        IPage<User> userIpage = new Page<>(page, 3);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "username", "photo", "rating", "win_count", "lose_count", "draw_count").orderByDesc("rating").orderByAsc("id");
        IPage<User> result = userMapper.selectPage(userIpage, queryWrapper);
        List<User> users = result.getRecords();
        Map<String,Object> resp = new HashMap<>();
        resp.put("users",users);
        resp.put("total", result.getTotal());
        return resp;
    }
}
