package com.zrlog.data.util;

import com.zrlog.common.vo.LockVO;
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

    public List<LockVO> getLocks() throws Exception {
        List<LockVO> lockList = new ArrayList<>();
        for (Map<String, Object> stringObjectMap : new WebSite().queryListWithParams("select * from website where name like ?", DistributedLock.LOCK_PREFIX + "%")) {
            LockVO lockVO = new LockVO();
            lockVO.setName(((String) stringObjectMap.get("name")).replaceFirst(DistributedLock.LOCK_PREFIX, ""));
            lockVO.setRemark((String) stringObjectMap.get("remark"));
            lockList.add(lockVO);
        }
        return lockList;
    }

    public void releaseLock(String lockKey) {
        new DistributedLock(lockKey).unlock();
    }
}
