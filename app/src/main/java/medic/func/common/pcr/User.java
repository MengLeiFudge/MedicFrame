package medic.func.common.pcr;

import lombok.Data;

import java.io.Serializable;

/**
 * @author MengLeiFudge
 */
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long group;
    private final long qq;
    /**
     * 今日sl状态，true表示未使用sl.
     */
    private boolean haveSl = true;

    User(long group, long qq) {
        this.group = group;
        this.qq = qq;
    }
}
