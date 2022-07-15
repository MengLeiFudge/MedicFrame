package medic.func.common.sakura.character.monsters;

public class JYSLM extends Monster {

    public JYSLM() {
        super("精英史莱姆", 5, 200, 100, 1000,
                10, 0, 5, 3, 110);
        setPhyCritRate(0.2);
        setMetal(0.9);
        setWood(0.9);
        setWater(0);
        setFire(1.5);
        setEarth(0.9);
    }

}