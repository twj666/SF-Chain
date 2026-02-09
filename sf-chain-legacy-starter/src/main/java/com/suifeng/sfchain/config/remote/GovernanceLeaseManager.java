package com.suifeng.sfchain.config.remote;

import lombok.extern.slf4j.Slf4j;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 治理租约锁（基于文件锁）
 */
@Slf4j
public class GovernanceLeaseManager {

    public enum AcquireMode {
        DISABLED,
        REMOTE,
        LOCAL,
        NONE
    }

    private final boolean enabled;
    private final boolean remoteLeaseEnabled;
    private final int remoteLeaseTtlSeconds;
    private final String leaseOwner;
    private final RemoteConfigClient remoteConfigClient;
    private final String lockFile;
    private FileChannel channel;
    private FileLock lock;
    private String remoteLeaseToken;
    private volatile AcquireMode lastAcquireMode = AcquireMode.NONE;

    public GovernanceLeaseManager(
            boolean enabled,
            boolean remoteLeaseEnabled,
            int remoteLeaseTtlSeconds,
            String leaseOwner,
            RemoteConfigClient remoteConfigClient,
            String lockFile) {
        this.enabled = enabled;
        this.remoteLeaseEnabled = remoteLeaseEnabled;
        this.remoteLeaseTtlSeconds = remoteLeaseTtlSeconds;
        this.leaseOwner = leaseOwner;
        this.remoteConfigClient = remoteConfigClient;
        this.lockFile = lockFile;
    }

    public synchronized boolean tryAcquire() {
        if (!enabled) {
            lastAcquireMode = AcquireMode.DISABLED;
            return true;
        }
        lastAcquireMode = AcquireMode.NONE;
        if (remoteLeaseEnabled && remoteConfigClient != null) {
            try {
                java.util.Optional<String> leaseToken = remoteConfigClient.tryAcquireGovernanceLease(
                        leaseOwner,
                        Math.max(remoteLeaseTtlSeconds, 5)
                );
                if (leaseToken.isPresent()) {
                    remoteLeaseToken = leaseToken.get();
                    lastAcquireMode = AcquireMode.REMOTE;
                    return true;
                }
            } catch (Exception ex) {
                log.debug("远程治理租约获取失败: {}", ex.getMessage());
            }
        }
        if (lock != null && lock.isValid()) {
            lastAcquireMode = AcquireMode.LOCAL;
            return true;
        }
        try {
            Path path = Paths.get(lockFile);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            channel = FileChannel.open(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
            );
            lock = channel.tryLock();
            boolean acquired = lock != null && lock.isValid();
            if (acquired) {
                lastAcquireMode = AcquireMode.LOCAL;
            }
            return acquired;
        } catch (Exception ex) {
            log.debug("治理租约获取失败: {}", ex.getMessage());
            return false;
        }
    }

    public AcquireMode getLastAcquireMode() {
        return lastAcquireMode;
    }

    public synchronized void release() {
        if (remoteLeaseToken != null && remoteConfigClient != null) {
            try {
                remoteConfigClient.releaseGovernanceLease(remoteLeaseToken);
            } catch (Exception ex) {
                log.debug("远程治理租约释放失败: {}", ex.getMessage());
            } finally {
                remoteLeaseToken = null;
            }
        }
        try {
            if (lock != null && lock.isValid()) {
                lock.release();
            }
        } catch (Exception ignore) {
        } finally {
            lock = null;
        }
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (Exception ignore) {
        } finally {
            channel = null;
        }
    }
}
