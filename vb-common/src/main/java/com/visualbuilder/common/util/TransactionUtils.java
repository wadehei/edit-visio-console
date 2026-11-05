package com.visualbuilder.common.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务工具 — 保证副作用（消息发送、缓存失效、外部调用等）在事务提交后执行，
 * 避免「事务回滚但副作用已生效」造成的数据不一致。
 */
public final class TransactionUtils {

    private TransactionUtils() {
    }

    /**
     * 在事务提交后执行动作；若当前不在事务上下文中则立即执行。
     * 事务回滚时不会执行该动作（与业务最终状态保持一致）。
     */
    public static void executeAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /**
     * 在事务回滚（或未提交）时执行补偿动作，例如清理已上传但未落库的存储文件。
     * 事务成功提交时不执行；若当前不在事务上下文中，则视为已提交、不执行补偿。
     */
    public static void executeOnRollback(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        action.run();
                    }
                }
            });
        }
    }
}
