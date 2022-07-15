package medic.func.common.arc.werewolf;

import lombok.Data;

import java.io.Serializable;

/**
 * @author MengLeiFudge
 */
@Data
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private long qq;

    Player(long qq) {
        this.qq = qq;
    }
}
