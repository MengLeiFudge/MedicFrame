package medic.func.common.sakura.character.monsters;

public class SLM extends Monster {

    public SLM() {
        super("史莱姆", 1, 50, 50, 300,
                5, 0, 0, 0, 80);
        setPhyCritRate(0.1);
        setWater(0.5);
        setFire(2.0);
    }

}
