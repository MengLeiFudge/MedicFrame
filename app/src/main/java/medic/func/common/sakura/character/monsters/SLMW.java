package medic.func.common.sakura.character.monsters;

public class SLMW extends Monster {

    public SLMW() {
        super("史莱姆王", 10, 2000, 100, 6000,
                50, 0, 20, 10, 140);
        setPhyCritRate(0.3);
        setMetal(0.7);
        setWood(0.7);
        setWater(-1);
        setFire(1.2);
        setEarth(0.7);
    }

}
