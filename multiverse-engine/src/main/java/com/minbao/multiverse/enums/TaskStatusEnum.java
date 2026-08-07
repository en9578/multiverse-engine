package com.minbao.multiverse.enums;

/**
 * 任务状态机。
 * 合法流转：CREATED → COLLECTING → GENERATING → EXPLORING → SETTLING → DONE
 * 任意状态可 FAILED。
 */
public enum TaskStatusEnum {
    CREATED,
    COLLECTING,
    GENERATING,
    EXPLORING,
    SETTLING,
    DONE,
    FAILED;

    /**
     * 校验当前状态能否转移到目标状态。
     * @throws IllegalStateException 若转移非法
     */
    public void nextStatus(TaskStatusEnum target) {
        if (target == FAILED) return; // 任意状态均可失败
        switch (this) {
            case CREATED -> require(target == COLLECTING, "CREATED → " + target);
            case COLLECTING -> require(target == GENERATING, "COLLECTING → " + target);
            case GENERATING -> require(target == EXPLORING, "GENERATING → " + target);
            case EXPLORING -> require(target == SETTLING, "EXPLORING → " + target);
            case SETTLING -> require(target == DONE, "SETTLING → " + target);
            case DONE, FAILED -> throw new IllegalStateException("终态不可转移: " + this + " → " + target);
        }
    }

    private static void require(boolean ok, String msg) {
        if (!ok) throw new IllegalStateException("非法状态迁移: " + msg);
    }
}
