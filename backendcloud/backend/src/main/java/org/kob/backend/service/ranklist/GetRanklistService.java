package org.kob.backend.service.ranklist;

import java.util.Map;

public interface GetRanklistService {
    Map<String, Object> getRanklist(Integer page);
}
