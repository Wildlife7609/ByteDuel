package org.kob.backend.service.impl.record;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.kob.backend.mapper.RecordMapper;
import org.kob.backend.mapper.UserMapper;
import org.kob.backend.pojo.User;
import org.kob.backend.pojo.Record;
import org.kob.backend.service.record.GetRecordListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GetRecordListServiceImpl implements GetRecordListService {
    @Autowired
    private RecordMapper recordMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Map<String, Object> getList(Integer page) {
        IPage<Record> recordPage = new Page<>(page, 10);
        QueryWrapper<Record> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        IPage<Record> result = recordMapper.selectPage(recordPage, queryWrapper);
        List<Record> records = result.getRecords();

        List<Map<String, Object>> items = new ArrayList<>();
        for (Record record : records) {
            User userA = userMapper.selectById(record.getAId());
            User userB = userMapper.selectById(record.getBId());
            Map<String, Object> item = new HashMap<>();
            item.put("a_username", userA.getUsername());
            item.put("b_username", userB.getUsername());
            item.put("a_photo", userA.getPhoto());
            item.put("b_photo", userB.getPhoto());
            item.put("record", record);
            items.add(item);
        }

        
        Map<String, Object> resp = new HashMap<>();
        resp.put("records", items);
        resp.put("total", result.getTotal());
        return resp;
    }
}
