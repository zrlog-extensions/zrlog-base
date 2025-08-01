package com.zrlog.data.util;

import com.zrlog.data.dto.LockDTO;
import com.zrlog.data.service.DistributedLock;
import com.zrlog.model.WebSite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DistributedLockManager {

    private static final DistributedLockManager instance = new DistributedLockManager();

    private DistributedLockManager() {

    }

    public static DistributedLockManager getInstance() {
        return instance;
    }

    public List<LockDTO> getLocks() throws Exception {
        List<LockDTO> lockList = new ArrayList<>();
        for (Map<String, Object> stringObjectMap : new WebSite().queryListWithParams("select * from website where name like ?", DistributedLock.LOCK_PREFIX + "%")) {
            LockDTO lockDTO = new LockDTO();
            lockDTO.setName(((String) stringObjectMap.get("name")).replaceFirst(DistributedLock.LOCK_PREFIX, ""));
            lockDTO.setRemark((String) stringObjectMap.get("remark"));
            lockList.add(lockDTO);
        }
        return lockList;
    }

    public void releaseLock(String lockKey) {
        new DistributedLock(lockKey).unlock();
    }
}
