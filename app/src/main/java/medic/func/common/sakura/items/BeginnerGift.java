package medic.func.common.sakura.items;

import medic.func.common.sakura.character.player.Player;

import java.util.List;

public class BeginnerGift extends Item {

    public BeginnerGift() {
        super("新手礼包", 1);
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }

    @Override
    public boolean use(Player player) {
        List<Item> itemList = player.getItems();
        for (Item item : itemList) {
            if (item.name.equals(this.name)) {
                return false;
            }
        }
        itemList.add(new Money(1000));
        return true;
    }

}
