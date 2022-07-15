package medic.func.common.sakura;

import medic.core.Api;
import medic.core.FuncProcess;
import medic.core.Main;
import medic.func.common.sakura.character.player.Player;

import java.text.DecimalFormat;

import static medic.core.Api.getAtQQ;
import static medic.core.Api.msgSource;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.ERR_LONG;
import static medic.func.common.sakura.SakuraUtils.getPlayer;
import static medic.func.common.sakura.SakuraUtils.save;
import static medic.func.common.sakura.character.player.Player.DianType;

/**
 * @author MengLeiFudge
 */
public class Process extends FuncProcess {
    public Process(Main.Func func) {
        super(func);
        menuList.add("落樱之都");
    }

    @Override
    public void menu() {
        send("-===\uD83C\uDF38落樱之都\uD83C\uDF38===-\n" +
                "\uD83C\uDF38个人信息◇人物加点\uD83C\uDF38\n" +
                "\uD83C\uDF38我的背包◇我的任务\uD83C\uDF38\n" +
                "\uD83C\uDF38装备强化◇落樱商城\uD83C\uDF38\n" +
                "\uD83C\uDF38单人副本◇魔塔挑战\uD83C\uDF38\n" +
                "\uD83C\uDF38多人副本◇竞技战斗\uD83C\uDF38\n" +
                "\uD83C\uDF38公会争霸◇姻缘系统\uD83C\uDF38\n" +
                "\uD83C\uDF38职业升阶◇新的开始\uD83C\uDF38\n" +
                "\uD83C\uDF38排行榜单◇竞猜娱乐\uD83C\uDF38\n" +
                "\uD83C\uDF38CDK 兑换◇后台管理\uD83C\uDF38\n" +
                "落樱之都更新日期：20210101\n" +
                "发送◇更新日志◇以查看世界变化\n" +
                "发送◇玩法◇查看游戏各功能的内容");
        sleep(300);
        send("标识说明：[g]群聊可用 [p]私聊可用 [a]仅Bot管理员可用\n" +
                "[gp]注册xxx：注册\n" +
                "[gp]改名xxx：改名\n" +
                "[gp]个人信息\n" +
                "[a][gp]加经验xxx：获取经验\n" +
                "[a][gp]嘤xxx：获取樱币\n" +
                "[gp]回复\n" +
                "[gp]加点\n" +
                "[g]其他指令暂定。"
        );
    }

    @Override
    public boolean process() {
        if (!textMsg.contains("@")) {
            if (textMsg.matches("落樱之都|菜单" + func)) {
                send("-===\uD83C\uDF38落樱之都\uD83C\uDF38===-\n" +
                        "\uD83C\uDF38个人信息◇人物加点\uD83C\uDF38\n" +
                        "\uD83C\uDF38我的背包◇我的任务\uD83C\uDF38\n" +
                        "\uD83C\uDF38装备强化◇落樱商城\uD83C\uDF38\n" +
                        "\uD83C\uDF38单人副本◇魔塔挑战\uD83C\uDF38\n" +
                        "\uD83C\uDF38多人副本◇竞技战斗\uD83C\uDF38\n" +
                        "\uD83C\uDF38公会争霸◇姻缘系统\uD83C\uDF38\n" +
                        "\uD83C\uDF38职业升阶◇新的开始\uD83C\uDF38\n" +
                        "\uD83C\uDF38排行榜单◇竞猜娱乐\uD83C\uDF38\n" +
                        "\uD83C\uDF38CDK 兑换◇后台管理\uD83C\uDF38\n" +
                        "落樱之都更新日期：20210101\n" +
                        "发送◇更新日志◇以查看世界变化\n" +
                        "发送◇玩法◇查看游戏各功能的内容");
                sleep(300);
                send("标识说明：[g]群聊可用 [p]私聊可用 [a]仅Bot管理员可用\n" +
                        "[gp]注册xxx：注册\n" +
                        "[gp]改名xxx：改名\n" +
                        "[gp]个人信息\n" +
                        "[a][gp]加经验xxx：获取经验\n" +
                        "[a][gp]嘤xxx：获取樱币\n" +
                        "[gp]回复\n" +
                        "[gp]加点\n" +
                        "[g]其他指令暂定。"
                );
                return true;

            } else if (textMsg.matches("更新日志")) {
                send("目前只是做了个框架= =，需要各位大力支持，有意做数据策划的，可以私戳萌泪\n" +
                        "副本开放落樱平原五大难度，欢迎前来挑战");
                return true;
            } else if (textMsg.matches("玩法")) {
                send("◇属性有效度◇\n" +
                        "人物和怪物本身没有属性，只有金、木、水、火、土、日、月、灵八种百分比形式的属性有效度。\n" +
                        "目标没有属性时，属性有效度为目标属性有效度的均值；\n" +
                        "只有一种属性时，属性有效度为对应属性有效度；\n" +
                        "有多种属性时，属性有效度为能造成最大伤害（敌对关系）/能回复最多血量（友好关系）的值。\n" +
                        "数值乘属性有效度，即为最终结果数值。\n" +
                        "100％不变，200％翻倍，50％减半，0％免疫，-100％表示伤害与治疗互换，数值不变。");
                sleep(300);
                send("◇升阶与升星◇\n" +
                        "每次转职本身属性会有增强，而且等阶的压制将会提高实际造成的伤害。\n" +
                        "20级可以去掉见习称号，升为一阶正式职业；\n" +
                        "40级可以转为二阶职业，学习到特有的技能；\n" +
                        "70级可以转为三阶职业，技能特色更加明显。\n" +
                        "100级可以觉醒，获得强大的特殊能力，属于四阶职业；\n" +
                        "140级二次觉醒，加强特殊能力的效果，属于五阶职业；\n" +
                        "190级进入神之境界，有禁忌技能，属于六阶职业。\n" +
                        "其中六阶职业又分为零星到十星，分别对应190级-200级。\n" +
                        "每升一星都会有能力明显提升，升为10星将拥有无与伦比的能力。");
                sleep(300);
                send("◇转生◇\n" +
                        "转生会给予额外的属性点，并且扩大了技能的威力，还提升了等级上限。\n" +
                        "转生后可以重新选择职业。\n" +
                        "零转人物等级上限为99级；一转人物等级上限为139级；二转人物等级上限为189级；三转人物等级上限为200级。\n" +
                        "189级及以下同样等级、等阶的情况下，每多一转，能力会大约高出50％；\n" +
                        "190级及以上每升一星（也就是升一级），能力增加约20％。");
                sleep(300);
                send("◇经验◇\n" +
                        "经验有三种获得途径，一是刷副本打怪，二是做任务，三是使用加经验道具。\n" +
                        "刷副本打怪获得的经验是动态的。公式为怪物基础经验乘除经验系数，\n" +
                        "其中经验系数只跟玩家和怪物的等级差有关，怪等级高为乘，等级低为除。\n" +
                        "怪物等级±3以内，系数为1.0；±6以内为1.2；±10以内为1.5；±15以内为2.0；\n" +
                        "±20以内为3.0；±30以内为5.0；超过±30为10.0。如果副本中死亡，将失去当前等级10％的经验。\n" +
                        "任务就不说了，你们都懂。加经验道具只有通过cdk兑换，或者是转生赠送这两种方式获得。");
                return true;
            } else if (textMsg.matches("注册.+")) {
                String name = textMsg.substring(2).replaceAll("\\s", "");
                if (name.length() > 0) {
                    if (name.length() >= 10) {
                        name = name.substring(0, 10);
                        send(qq, "名字太长了，使用前十个字符~");
                    }
                    createRole(name);
                } else {
                    send(qq, "要有名字哦！");
                }
                return true;
            } else if (textMsg.matches("改名.+")) {
                String name = textMsg.substring(2).replaceAll("\\s", "");
                if (name.length() > 0) {
                    if (name.length() >= 10) {
                        name = name.substring(0, 10);
                        send(qq, "新名字太长了，使用前十个字符~");
                    }
                    rename(name);
                } else {
                    send(qq, "要有新名字哦！");
                }
                return true;
            } else if (textMsg.matches("个人信息")) {
                showMyInfo();
                return true;
            } else if (textMsg.matches("加经验[0-9]+")) {
                addExp(Integer.parseInt(textMsg.substring(3)));
                return true;
            } else if (textMsg.matches("嘤[0-9]+")) {
                addMoney(Integer.parseInt(textMsg.substring(1)));
                return true;
            } else if (textMsg.matches("恢复|回复")) {
                resetRole();
                return true;
            } else if (textMsg.matches("休息")) {
                //resetRole();
                return true;
            } else if (textMsg.matches("结束休息")) {
                //resetRole();
                return true;
            } else if (textMsg.matches("加[0-9]+(力量|智力|体质|敏捷|魅力)")
                    || textMsg.matches("加(力量|智力|体质|敏捷|魅力) *[0-9]+")) {
                int num = Integer.parseInt(textMsg.split("\\D+")[0]);
                if (textMsg.contains("力量")) {
                    addDian(DianType.LI, num);
                } else if (textMsg.contains("智力")) {
                    addDian(DianType.ZHI, num);
                } else if (textMsg.contains("体质")) {
                    addDian(DianType.TI, num);
                } else if (textMsg.contains("敏捷")) {
                    addDian(DianType.MIN, num);
                } else if (textMsg.contains("魅力")) {
                    addDian(DianType.MEI, num);
                } else {
                    // nothing
                }
                return true;
            }  /* else if (isAdmin) {
                if (textMsg.matches("取消全部")) {
                    if (msgSource != Api.MsgSource.GROUP) {
                        return true;
                    }
                    // cancelAll();
                    return true;
                } else if (textMsg.matches("设置[A-Ca-c]面? *[1-5] +[0-9]+[Ww]?")) {
                    if (msgSource != Api.MsgSource.GROUP) {
                        return true;
                    }
                    return true;
                }
            }*/
        } else {
            if (getAtQQ() == ERR_LONG || msgSource != Api.MsgSource.GROUP) {
                return false;
            }
            /*
            if (textMsg.matches("加入@.+")) {
                //joinGuild(getAtQQ());
                return true;
            } else if (isAdmin) {
                if (textMsg.matches("(删除|清除)全部@.+")) {
                    if (getAtQQ() == getRobotQQ()) {
                        // clear();
                        return true;
                    } else {
                        send(qq, "删除本群pcr信息需艾特Bot！");
                    }
                }
            }*/
        }
        return false;
    }

    private void createRole(String name) {
        Player player = getPlayer(qq);
        if (player == null) {
            player = new Player(qq);
            player.setName(name);
            save(player);
            send(qq, "已创建角色【" + name + "】！");
        } else {
            send(qq, "已有角色，无法创建！");
        }
    }

    private void rename(String newName) {
        Player player = getPlayer(qq);
        if (player == null) {
            send(qq, "你还没注册呢！\n" +
                    "在落樱之都遨游需要通行证哦！\n" +
                    "据说发送【注册+昵称】就可以获得通行证辣~");
            return;
        }
        player.setName(newName);
        send(qq, "已更改昵称为" + newName);
        save(player);
    }

    private void showMyInfo() {
        Player player = getPlayer(qq);
        if (player == null) {
            send(qq, "你还没注册呢！\n" +
                    "在落樱之都遨游需要通行证哦！\n" +
                    "据说发送【注册+昵称】就可以获得通行证辣~");
            return;
        }
        DecimalFormat format = new DecimalFormat("#0.00%");
        send(qq, "-===\uD83C\uDF38落樱之都\uD83C\uDF38===-\n" +
                "Lv." + player.getLevel() + " " + player.getName() + "\n" +
                player.getZhuanType() + player.getJieType() + player.getZhiType() + "\n" +

                "生命：" + player.getHp() + "/" + player.getMaxHp() + "\n" +
                "魔力：" + player.getMp() + "/" + player.getMaxMp() + "\n" +
                "经验：" + player.getExp() + "/" + player.getMaxExp() + "\n" +
                "樱币：" + player.getMoney() + "\n" +

                "攻击：" + player.getPhyAtk() + "   法强：" + player.getMagAtk() + "\n" +
                "物理暴击：" + format.format(player.getPhyCritRate()) +
                "/" + format.format(player.getPhyCritEffect()) + "\n" +
                "法术暴击：" + format.format(player.getMagCritRate()) +
                "/" + format.format(player.getMagCritEffect()) + "\n" +

                "物防：" + player.getPhyDef() + "   法防：" + player.getMagDef() + "\n" +
                "速度：" + player.getSpeed() + "\n" +
                //"物理吸血： 法术吸血   幸运：\n" +

                "金" + format.format(player.getMetal()) +
                "   木" + format.format(player.getWood()) + "\n" +
                "水" + format.format(player.getWater()) +
                "   火" + format.format(player.getFire()) + "\n" +
                "土" + format.format(player.getEarth()) +
                "   日" + format.format(player.getSun()) + "\n" +
                "月" + format.format(player.getMoon()) +
                "   灵" + format.format(player.getSoul())
        );
        save(player);
    }

    private void addExp(int num) {
        Player player = getPlayer(qq);
        if (player == null) {
            send(qq, "你还没注册呢！\n" +
                    "在落樱之都遨游需要通行证哦！\n" +
                    "据说发送【注册+昵称】就可以获得通行证辣~");
            return;
        }
        player.addExp(num);
        int level = player.getLevel();
        player.levelUp();
        int level2 = player.getLevel();
        if (level != level2) {
            send(qq, "升至" + level2 + "级，\n当前经验：" + player.getExp() + "/" + player.getMaxExp());
        }
        save(player);
    }

    private void addMoney(int num) {
        Player player = getPlayer(qq);
        if (player == null) {
            send(qq, "你还没注册呢！\n" +
                    "在落樱之都遨游需要通行证哦！\n" +
                    "据说发送【注册+昵称】就可以获得通行证辣~");
            return;
        }
        player.addMoney(num);
        send(qq, "当前樱币：" + player.getMoney());
        save(player);
    }

    private void resetRole() {
        Player player = getPlayer(qq);
        if (player == null) {
            send(qq, "你还没注册呢！\n" +
                    "在落樱之都遨游需要通行证哦！\n" +
                    "据说发送【注册+昵称】就可以获得通行证辣~");
            return;
        }
        player.setHp(player.getMaxHp());
        player.setMp(player.getMaxMp());
        send(qq, "hp mp 已回满");
        save(player);
    }

    private void addDian(DianType type, int num) {
        Player player = getPlayer(qq);
        if (player == null) {
            send(qq, "你还没注册呢！\n" +
                    "在落樱之都遨游需要通行证哦！\n" +
                    "据说发送【注册+昵称】就可以获得通行证辣~");
            return;
        }
        if (num > player.getDian()) {
            send(qq, "剩余属性点" + player.getDian() + "，不足" + num);
            return;
        }
        switch (type) {
            case LI:
                player.setLiliang(player.getLiliang() + num);
                player.setLiliangAdd(player.getLiliangAdd() + num);
                break;
            case ZHI:
                player.setZhili(player.getZhili() + num);
                player.setZhiliAdd(player.getZhiliAdd() + num);
                break;
            case TI:
                player.setTizhi(player.getTizhi() + num);
                player.setTizhiAdd(player.getTizhiAdd() + num);
                break;
            case MIN:
                player.setMinjie(player.getMinjie() + num);
                player.setMinjieAdd(player.getMinjieAdd() + num);
                break;
            case MEI:
                player.setMeili(player.getMeili() + num);
                player.setMeiliAdd(player.getMeiliAdd() + num);
                break;
            default:
                break;
        }
        send(qq, "加点完毕");
        save(player);
    }
}
