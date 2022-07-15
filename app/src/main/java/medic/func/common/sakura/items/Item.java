package medic.func.common.sakura.items;

import lombok.Data;
import medic.func.common.sakura.character.player.Player;

import java.io.Serializable;

/**
 * @author MengLeiFudge
 */
@Data
public abstract class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    Item(String name, long buyPrice) {
        this.name = name;
        this.buyPrice = buyPrice;
    }

    Item(String name) {
        this.name = name;
        // 不可购买
        buyPrice = -1;
    }

    String name;
    /**
     * 商品是否可以在商店中买到？
     */
    long buyPrice;

    /**
     * 玩家能否使用该道具.
     *
     * @param player
     * @return
     */
    public abstract boolean canUse(Player player);

    /**
     * 玩家使用该道具会发生什么.
     *
     * @param player
     * @return
     */
    public abstract boolean use(Player player);
}
