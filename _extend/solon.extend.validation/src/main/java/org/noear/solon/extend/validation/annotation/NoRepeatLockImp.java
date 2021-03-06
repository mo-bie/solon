package org.noear.solon.extend.validation.annotation;

import org.noear.solon.core.CacheService;
import org.noear.solon.core.XBridge;

/**
 * 锁的默认实现
 *
 * 只适合地锁；分布式环境需要把锁实现换掉
 * @author noear
 * @since 1.0
 * */
public class NoRepeatLockImp implements NoRepeatLock {
    private static NoRepeatLock _global;

    public static NoRepeatLock global() {
        if (_global == null) {
            _global = new NoRepeatLockImp();
        }

        return _global;
    }

    public static void globalSet(NoRepeatLock lock) {
        if (lock != null) {
            _global = lock;
        }
    }

    private CacheService _cache = XBridge.cacheServiceGet("");

    public boolean tryLock(String key, int inSeconds) {
        String key2 = "lock." + key;

        synchronized (key2.intern()) {
            Object tmp = _cache.get(key2);

            if (tmp == null) {
                //如果还没有，则锁成功
                _cache.store(key2, "_", inSeconds);
                return true;
            } else {
                return false;
            }
        }
    }
}
