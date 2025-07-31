package com.zrlog.data.service;

import com.hibegin.common.util.LoggerUtil;
import com.zrlog.model.WebSite;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

/**
 * 简洁的分布式锁，解决一些特殊情况下的并发问题（非高效的方式）
 */
public class DistributedLock implements Lock {

    private static final Logger LOGGER = LoggerUtil.getLogger(DistributedLock.class);
    private final String lockKey;
    private final String rawLockKey;

    public DistributedLock(String lockKey) {
        this.lockKey = "lock." + lockKey;
        this.rawLockKey = lockKey;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long totalWaitTime = unit.toMillis(time);
        if (totalWaitTime > 600 * 1000) {
            throw new IllegalArgumentException("waitTime too long");
        }
        if (totalWaitTime <= 0) {
            throw new IllegalArgumentException("waitTime must be a positive number");
        }
        //简单数据库锁，不宜时间太短
        int seek = 200;
        while (true) {
            if (tryLock()) {
                return true;
            }
            totalWaitTime -= seek;
            if (totalWaitTime <= 0) {
                return false;
            }
            Thread.sleep(seek);
        }
    }

    @Override
    public void lock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lockInterruptibly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        try {
            return new WebSite().set("name", lockKey).set("remark", "DistributedLock").set("value", "locked").save();
        } catch (SQLException e) {
            LOGGER.warning("tryLock " + rawLockKey + " error " + e.getMessage());
            //throw new RuntimeException(e);
            return false;
        }
    }

    @Override
    public void unlock() {
        try {
            new WebSite().set("name", lockKey).delete();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }
}
