package cn.wangguan.miningassistant.ui;

import cn.wangguan.miningassistant.ClientActions;
import cn.wangguan.miningassistant.MiningAssistantClient;
import cn.wangguan.miningassistant.EmergencyController;
import cn.wangguan.miningassistant.model.EmergencyPreferences;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

final class EmergencyScreen extends HelperScreen {
    private int threshold=6;
    EmergencyScreen(Screen parent){super("低血量自动回家",parent);}
    private EmergencyPreferences.Home home(){
        var settings=MiningAssistantClient.emergencyPreferences;
        return settings==null || !settings.readable() || minecraft.player==null?null:settings.home(ClientActions.serverScope(minecraft));
    }
    @Override protected void init(){
        super.init();
        var home=home();if(home!=null)threshold=home.threshold();
        boolean compact=height<200;
        paragraph("开启后血量过低就回家；如果正在自动工作，会先停工。满血返回后仍停工。OP 不需要服务端传送插件。",layout.x(),35,layout.width(),2,MUTED);
        String location=home==null?"还没有设置家：站在家里，点记住当前位置。":
            String.format(Locale.ROOT,"家：%.1f, %.1f, %.1f（%s）；%s",home.x(),home.y(),home.z(),home.dimension(),home.enabled()?"已开启":"未开启");
        paragraph(location,layout.x(),compact?57:63,layout.width(),1,MUTED);
        int sliderY=compact?70:80,rowY=compact?95:105,extraY=compact?120:130;
        var slider=new AbstractSliderButton(layout.x(),sliderY,layout.width(),20,Component.empty(),(threshold/2-1)/8.0){
            {updateMessage();}
            @Override protected void updateMessage(){setMessage(Component.literal("剩余 "+(1+(int)Math.round(value*8))+" 颗心时回家"));}
            @Override protected void applyValue(){
                int next=2*(1+(int)Math.round(value*8));if(next==threshold)return;
                threshold=next;var current=home();
                if(current!=null && !save(new EmergencyPreferences.Home(current.dimension(),current.x(),current.y(),current.z(),current.yaw(),current.pitch(),threshold,current.enabled()))){
                    threshold=current.threshold();value=(threshold/2-1)/8.0;updateMessage();
                }
            }
        };
        addRenderableWidget(slider);
        int half=(layout.width()-6)/2;boolean narrow=layout.width()<240;
        button(narrow?"记家":"记住当前位置",layout.x(),rowY,half,()->{
            if(minecraft.player==null || minecraft.level==null || minecraft.getConnection()==null){status="请先进入世界。";return;}
            if(EmergencyController.active()){status="请先取消正在进行的回家。";return;}
            var p=minecraft.player;
            if(save(new EmergencyPreferences.Home(ClientActions.dimension(minecraft),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot(),threshold,false))){
                status="家已记住；请检查后开启。";rebuildWidgets();
            }
        });
        button(home!=null && home.enabled()?"关闭":"开启",layout.x()+half+6,rowY,half,()->{
            var current=home();
            if(current==null){status="先站在家里，点击记住当前位置。";return;}
            if(!current.enabled()){
                if(!EmergencyController.canTeleportTo(minecraft,current.dimension())){
                    boolean crossDimension=!ClientActions.dimension(minecraft).equals(current.dimension());
                    status=crossDimension
                        ?"当前服务器没有可用的跨维度传送命令；确认已是 OP 后重新进服，普通玩家需要服务端传送插件。"
                        :"当前服务器没有 /tp 权限；确认已是 OP 后重新进服，普通玩家需要服务端传送插件。";
                    return;
                }
            }
            if(save(new EmergencyPreferences.Home(current.dimension(),current.x(),current.y(),current.z(),current.yaw(),current.pitch(),threshold,!current.enabled()))){
                if(current.enabled())EmergencyController.cancel(minecraft);
                status=current.enabled()?"已关闭自动回家。": "已开启；OP 可跨维度使用原版命令，普通玩家需要服务端传送插件。";
                rebuildWidgets();
            }
        });
        button("回血小贴士",layout.x(),extraY,half,()->minecraft.gui.setScreen(new RecoveryTipScreen(this)));
        var cancel=button(narrow?"取消返程":"取消本次自动返回",layout.x()+half+6,extraY,half,()->{EmergencyController.cancel(minecraft);status="已取消本次自动返回。";});
        cancel.active=EmergencyController.active();
        button(narrow?"返回":"返回上一页",layout.x(),layout.footerY(),layout.width(),this::onClose);
    }
    private boolean save(EmergencyPreferences.Home home){
        try{
            if(MiningAssistantClient.emergencyPreferences==null || minecraft.player==null)throw new IllegalStateException();
            MiningAssistantClient.emergencyPreferences.save(ClientActions.serverScope(minecraft),home);
            return true;
        }catch(Exception e){status="保存失败，原来的设置还在。";return false;}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        super.extractRenderState(g,x,y,delta);
        if(status.isEmpty() && EmergencyController.active())
            g.text(font,font.plainSubstrByWidth(EmergencyController.status(),layout.width()),layout.x(),statusY(),MUTED,false);
    }
}

