package medic.func.common.pcr;

import lombok.Data;

import java.io.Serializable;

import static medic.core.Api.msgTime;

/**
 * @author MengLeiFudge
 */
@Data
public class Atk implements Serializable {
    private static final long serialVersionUID = 1L;

    private long qq;
    /**
     * 出刀时间.
     */
    private long time = msgTime;
    private int bossLoop;
    private int bossIndex;
    private Boss.Stage bossStage;
    private int damage;
    private AtkType atkType;

    public enum AtkType {
        /**
         * 普通刀，记为1刀.
         */
        COMMON,
        /**
         * 尾刀，记为0.5刀.
         */
        KILL,
        /**
         * 补偿刀，记为0.5刀.
         */
        COMPENSATE;

        public double toDouble() {
            if (this == COMMON) {
                return 1.0;
            } else {
                return 0.5;
            }
        }
    }

    /**
     * 于修改boss前调用.
     */
    Atk(Apply apply, int damage, int bossLoop, int bossIndex, Boss.Stage bossStage, boolean killBoss) {
        this.qq = apply.getQq();
        this.bossLoop = bossLoop;
        this.bossIndex = bossIndex;
        this.bossStage = bossStage;
        this.damage = damage;
        if (apply.getApplyType() == Apply.ApplyType.COMMON) {
            // 普通刀可以转普通刀或尾刀（多出来一个补偿刀）
            if (killBoss) {
                atkType = AtkType.KILL;
            } else {
                atkType = AtkType.COMMON;
            }
        } else {
            // 补偿刀只能转补偿刀，即使击杀也不会产生新的补偿刀
            atkType = AtkType.COMPENSATE;
        }
    }
}
