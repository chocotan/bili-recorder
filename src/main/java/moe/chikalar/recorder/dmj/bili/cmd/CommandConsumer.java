package moe.chikalar.recorder.dmj.bili.cmd;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.Date;

public class CommandConsumer implements Consumer<BaseCommand> {
    private final Subject<GiftDto> giftSubject;
    private final Subject<DanmuDto> danmuSubject;
    private final PublishSubject<UserEntryDto> userEntrySubject;

    public CommandConsumer(Subject<DanmuDto> danmuSubject,
                           Subject<GiftDto> giftSubject,
                           PublishSubject<UserEntryDto> userEntrySubject) {
        this.danmuSubject = danmuSubject;
        this.giftSubject = giftSubject;
        this.userEntrySubject = userEntrySubject;
    }

    @Override
    public void accept(BaseCommand cmd) throws Exception {
        switch (cmd.getCmd()) {
            case "SEND_GIFT":
                JSONObject dataObj = cmd.getData();
                Long price = dataObj.getLong("price");
                Long num = dataObj.getLong("num");
                Long giftId = dataObj.getLong("giftId");
                Long uid = dataObj.getLong("uid");
                String giftName = dataObj.getString("giftName");
                String uname = dataObj.getString("uname");
                GiftDto giftDto = new GiftDto();
                giftDto.setGiftId(giftId);
                giftDto.setGiftName(giftName);
                giftDto.setUname(uname);
                giftDto.setUid(uid);
                giftDto.setNum(num);
                giftDto.setPrice(price);
                giftDto.setCommand(cmd);
                giftSubject.onNext(giftDto);
                break;
            case "DANMU_MSG":
                JSONArray info = cmd.getInfo();
                if (info != null) {
                    String msg = (String) info.get(1);
                    JSONArray userInfo = (JSONArray) info.get(2);
                    uid = Long.valueOf("" + userInfo.get(0));
                    uname = (String) userInfo.get(1);
                    DanmuDto danmuDto = new DanmuDto();
                    danmuDto.setMsg(msg);
                    danmuDto.setUid(uid);
                    danmuDto.setUname(uname);
                    danmuDto.setCommand(cmd);
                    try {
                        danmuDto.setBand((String) ((JSONArray) info.get(3)).get(1));
                        danmuDto.setBandLevel("" + ((JSONArray) info.get(3)).get(0));
                    } catch (Exception ignored) {
                    }
                    danmuSubject.onNext(danmuDto);
                }
                break;
            case "GUARD":
                JSONObject data = cmd.getData();
                uname = data.getString("username");
                giftName = data.getString("gift_name");
                // TODO 购买舰长


                break;
            case "ENTRY_EFFECT":
                // TODO 舰长进入直播间
                // {"copy_writing":"欢迎 <%扇宝直播感谢姬%> 进入直播间","effective_time":2,"privilege_type":0,"basemap_url":"https://i0.hdslb.com/bfs/live/mlive/586f12135b6002c522329904cf623d3f13c12d2c.png","business":3,"mock_effect":0,"highlight_color":"#FFF100","target_id":72041,"copy_writing_v2":"欢迎 <^icon^> <%扇宝直播感谢姬%> 进入直播间","max_delay_time":7,"priority":1,"web_effective_time":2,"uid":1107381871,"face":"https://i0.hdslb.com/bfs/face/6b2fbe8fbc423512879da127e0c4b9c2c52d486b.jpg","web_effect_close":0,"copy_color":"#000000","web_close_time":900,"show_avatar":1,"icon_list":[2],"id":136,"web_basemap_url":"https://i0.hdslb.com/bfs/live/mlive/586f12135b6002c522329904cf623d3f13c12d2c.png"}
                break;
            case "INTERACT_WORD":
                data = cmd.getData();
                Long timestamp = data.getLong("timestamp");
                uid = data.getLong("uid");
                uname = data.getString("uname");
                UserEntryDto entryDto = new UserEntryDto();
                entryDto.setTimestamp(new Date(timestamp));
                entryDto.setUname(uname);
                entryDto.setUid(uid);
                entryDto.setCommand(cmd);
                try {
                    String medal = data.getJSONObject("fans_medal").getString("medal_name");
                    String level = data.getJSONObject("fans_medal").getString("medal_level");
                    entryDto.setMedal(medal);
                    if (!"0".equals(level))
                        entryDto.setMedalLevel(level);
                    else{
                        entryDto.setMedal(null);
                        entryDto.setMedalLevel(null);
                    }
                } catch (Exception e) {
                }
                userEntrySubject.onNext(entryDto);
                // TODO 非舰长进入直播间
                // {"spread_info":"","uname":"无敌雷哥007","spread_desc":"","is_spread":0,"roomid":75241,"uid":1581489364,"score":1610632585589,"tail_icon":0,"fans_medal":{"medal_name":"","medal_color_border":0,"medal_color_end":0,"target_id":0,"icon_id":0,"medal_color":0,"special":"","score":0,"medal_color_start":0,"is_lighted":0,"medal_level":0,"anchor_roomid":0,"guard_level":0},"identities":[1],"contribution":{"grade":0},"msg_type":1,"uname_color":"","timestamp":1610632585}
                break;
            default:
                System.out.println(cmd.getCmd() + ": " + cmd.getData());
        }
    }
}
