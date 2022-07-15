package medic.func.common.pcr;

import lombok.Data;

import java.io.Serializable;

import static medic.core.Api.msgTime;

/**
 * @author MengLeiFudge
 */
@Data
public class Apply implements Serializable {
    private static final long serialVersionUID = 1L;

    private long qq;
    /**
     * 指示排该刀的人是否正在挂树.
     */
    private boolean isOnTree = false;
    /**
     * 指示该刀是否已出完.
     */
    private boolean finished = false;
    /**
     * 排刀时间.
     */
    private long time = msgTime;
    private ApplyType applyType;

    public enum ApplyType {
        /**
         * 普通刀，记为1刀.
         */
        COMMON,
        /**
         * 补偿刀，记为0刀.
         */
        COMPENSATE;

        int toInt() {
            if (this == COMMON) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    Apply(long qq, ApplyType applyType) {
        this.qq = qq;
        this.applyType = applyType;
    }
}
