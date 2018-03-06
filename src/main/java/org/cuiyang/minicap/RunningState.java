package org.cuiyang.minicap;

/**
 * 运行状态
 *
 * @author cy48576
 */
public enum RunningState {
    /** 就绪状态 */
    READY,
    /** 启动中 */
    STARTUP,
    /** 运行中 */
    RUNNING,
    /** 已关闭 */
    CLOSED,
}
