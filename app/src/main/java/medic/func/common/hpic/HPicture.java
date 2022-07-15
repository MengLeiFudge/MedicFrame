package medic.func.common.hpic;

import medic.core.Api;
import medic.core.FuncProcess;
import medic.core.Main;
import medic.core.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static medic.core.Api.addImg;
import static medic.core.Api.addText;
import static medic.core.Api.group;
import static medic.core.Api.isAdmin;
import static medic.core.Api.msgSource;
import static medic.core.Api.qq;
import static medic.core.Api.send;
import static medic.core.Api.textMsg;
import static medic.core.Utils.DEF_INT;
import static medic.core.Utils.getFile;
import static medic.core.Utils.getInt;
import static medic.core.Utils.getStrFromURL;
import static medic.core.Utils.logError;
import static medic.core.Utils.set;
import static medic.core.Utils.unicodeToUtf8;

/**
 * @author MengLeiFudge
 */
public class HPicture extends FuncProcess {
    public HPicture(Main.Func func) {
        super(func);
        int i = getInt(SETU_FILE, group);
        if (i == DEF_INT) {
            groupFlag = false;
            showImageFlag = false;
            save();
        } else {
            groupFlag = (i & (1 << 0)) == 1 << 0;
            showImageFlag = (i & (1 << 1)) == 1 << 1;
        }
    }

    //api来源：https://api.lolicon.app/#/setu

    private static final File SETU_FILE = getFile(Utils.Dir.SETTINGS, "setu.txt");
    private static boolean groupFlag;
    private static boolean showImageFlag;

    private void save() {
        int value = 0;
        if (groupFlag) {
            value += 1;
        }
        if (showImageFlag) {
            value += 1 << 1;
        }
        set(SETU_FILE, group, value);
    }

    enum R18Type {
        // 非r18
        NON_R18,
        // r18
        R18,
        // 混合
        MIXED;

        @Override
        public String toString() {
            if (this == NON_R18) {
                return "美图";
            } else if (this == R18) {
                return "色图";
            } else {
                return "混合图";
            }
        }

        public int getTypeIndex() {
            if (this == NON_R18) {
                return 0;
            } else if (this == R18) {
                return 1;
            } else {
                return 2;
            }
        }

        public static R18Type getR18TypeByStr(String s) {
            if (s.matches("美图")) {
                return R18Type.NON_R18;
            } else if (s.matches("[色涩蛇]图")) {
                return R18Type.R18;
            } else {
                return R18Type.MIXED;
            }
        }
    }

    /**
     * v1版本需要，现已无用.
     */
    static final String API_KEY = "589871545edc96be118785";

    @Override
    public void menu() {
        send("[g]群聊 [p]私聊 [a]仅管理员\n" +
                "[gp](来点)色图/涩图/蛇图(+数目)：获取指定数目的色图，默认1张\n" +
                "[gp](来点)色图+tag(+数目)：获取指定数目的包含tag的色图（可能获取不到足够多的数量，默认5张）\n" +
                "[a]开/关群色图：控制能否在群内展示色图消息\n" +
                "[a]开/关图片显示：控制图片消息是否包含缩略图\n" +
                "色图(r18)可换为美图(无r18)或混合(r18和非r18都有)\n" +
                "以下是一些指令示例：\n" +
                "【色图20】表示20张色图\n" +
                "【美图凯露】表示5张凯露的美图\n" +
                "【美图凯露20】表示20张凯露的美图\n" +
                "【美图 凯露 可可萝 10】表示10张同时包含凯露和可可萝的美图\n" +
                "【美图 凯露|可可萝 10】表示10张包含凯露或可可萝的美图\n" +
                "【色图 萝莉|少女 白丝|黑丝】表示5张(萝莉或少女)的(白丝或黑丝)的色图\n" +
                "至多3个tag，每个tag至多20个选项\n" +
                "群色图状态：" + (groupFlag ? "开启" : "关闭") + "\n" +
                "群图片显示状态：" + (showImageFlag ? "开启" : "关闭")
        );
    }


    @Override
    public boolean process() {
        // 先判断是否更改群色图设置
        if (isAdmin) {
            if (textMsg.matches("[开关]群色图")) {
                groupFlag = textMsg.startsWith("开");
                save();
                send("已" + (groupFlag ? "开启" : "关闭") + "群色图！");
                return true;
            } else if (textMsg.matches("[开关]图片显示")) {
                showImageFlag = textMsg.startsWith("开");
                save();
                if (showImageFlag) {
                    send("已开启图片显示！\n" +
                            "注意，开启此功能极有可能导致无法接收到消息！\n" +
                            "即使开启，r18图片也不会有缩略图显示~");
                } else {
                    send("已关闭图片显示！");
                }
                return true;
            }
        }
        if (!textMsg.matches("(来点)?([美色涩蛇]图|混合).*")) {
            return false;
        }
        if (textMsg.startsWith("来点")) {
            textMsg = textMsg.substring(2);
        }
        R18Type type = R18Type.getR18TypeByStr(textMsg.substring(0, 2));
        textMsg = textMsg.substring(2).trim();
        // 不带tag一张
        if ("".equals(textMsg)) {
            setu(type, 1, (String) null);
            return true;
        }
        // 不带tag多张
        if (textMsg.matches("[0-9]+")) {
            setu(type, Integer.parseInt(textMsg), (String) null);
            return true;
        }
        // 带tag，判断结尾是否有数字
        int num = 5;
        if (textMsg.matches(".*[0-9]+")) {
            String[] nums = textMsg.split("\\D+");
            int num0 = Integer.parseInt(nums[nums.length - 1]);
            String num0Str = num0 + "";
            if (textMsg.endsWith(num0Str)) {
                num = num0;
                textMsg = textMsg.substring(0, textMsg.length() - num0Str.length()).trim();
            }
        }
        // 将tag以空白字符分割
        String[] tags = textMsg.split("\\s+");
        setu(type, num, tags);
        return true;
    }

    private void setu(R18Type type, int num, String... searchTags) {
        if (num <= 0) {
            send(qq, "图片数目至少为1！");
            return;
        }
        if (msgSource == Api.MsgSource.GROUP && type != R18Type.NON_R18 && !groupFlag) {
            send("本群当前设置为群内只能查看非R18图片！\n请私聊发送指令QwQ");
            return;
        }
        StringBuilder apiUrl = new StringBuilder("https://api.lolicon.app/setu/v2?");
        apiUrl.append("r18=").append(type.getTypeIndex())
                .append("&size=original")
                .append("&size=regular")
                .append("&size=small")
                .append("&size=thumb")
                .append("&size=mini")
                .append("&num=").append(Math.min(num, 20));
        if (searchTags != null) {
            for (String tag : searchTags) {
                if (tag != null && !"".equals(tag)) {
                    apiUrl.append("&tag=").append(tag);
                }
            }
        }
        send(qq, "正在查找图片...");
        String s = getStrFromURL(apiUrl.toString(), null);
        try {
            JSONObject obj = new JSONObject(s);
            String code = obj.getString("error");
            if (!"".equals(code)) {
                send("Api 出错辣！\n" + code);
                return;
            }
            JSONArray data = obj.getJSONArray("data");
            if (data.length() == 0) {
                send("没有找到符合你要求的图片呢QAQ\n尝试减少一些tag吧！");
                return;
            }
            for (int i = 0; i < data.length(); i++) {
                addText("图片索引：" + (i + 1) + " / " + data.length() + "\n");
                JSONObject img = data.getJSONObject(i);
                JSONObject urls = img.getJSONObject("urls");
                String originalImgUrl = unicodeToUtf8(urls.getString("original"));
                String regularImgUrl = unicodeToUtf8(urls.getString("regular"));
                String smallImgUrl = unicodeToUtf8(urls.getString("small"));
                String thumbImgUrl = unicodeToUtf8(urls.getString("thumb"));
                String miniImgUrl = unicodeToUtf8(urls.getString("mini"));
                boolean r18 = img.getBoolean("r18");
                if (!r18 && showImageFlag) {
                    // 选取第三级别图片作为缩略图
                    addImg(smallImgUrl);
                    addText("\n");
                }
                StringBuilder sb = new StringBuilder();
                sb.append(originalImgUrl).append("\n")
                        .append(img.getString("title"))
                        .append("(PID ").append(img.getString("pid")).append(")\n")
                        .append("by ").append(img.getString("author"))
                        .append("(UID ").append(img.getString("uid")).append(")\n")
                        .append("tags：");
                JSONArray tags = img.getJSONArray("tags");
                for (int j = 0; j < tags.length(); j++) {
                    if (j != 0) {
                        sb.append("，");
                    }
                    sb.append(tags.getString(j));
                }
                addText(sb.toString());
                send();
                sleep(showImageFlag ? 3000 : 300);
            }
        } catch (JSONException e) {
            logError(e);
        }
    }
}
