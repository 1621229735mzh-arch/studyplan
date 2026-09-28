package com.kaoyan.study.settings.service;

/**
 * 复习阶段。
 *
 * <p>主学习阶段与集中复习阶段由本人主动切换；阶段不同只影响复习额度的使用，
 * 不改变“复习内容必须已加入复习”这一前提。
 */
public enum ReviewPhase {

    /** 主学习阶段：先学习，再按已学内容逐步建立复习清单。 */
    MAIN,

    /** 集中复习阶段：由本人主动切换并重新设置额度。 */
    INTENSIVE
}
