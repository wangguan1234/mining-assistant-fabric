package cn.wangguan.miningassistant.ui;
import cn.wangguan.miningassistant.RegionPick;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.screens.Screen;
import cn.wangguan.miningassistant.ClientActions;
import cn.wangguan.miningassistant.MiningAssistantClient;
import cn.wangguan.miningassistant.model.RegionVolume;
import net.minecraft.world.item.Items;

public final class RegionScreen extends HelperScreen {
    public RegionScreen(Screen parent) { super("木棍选区",parent); }
    @Override protected void init() {
        super.init();
        boolean compact=height<225;
        paragraph("拿木棍对准方块：左键选第一个角，右键选第二个角。选完按 "+MiningAssistantClient.OPEN.getTranslatedKeyMessage().getString()+" 回来。",layout.x(),36,layout.width(),compact?2:3,MUTED);
        paragraph("第一个角："+format(RegionPick.first())+"\n第二个角："+format(RegionPick.second()),layout.x(),compact?60:73,layout.width(),3,INK);
        paragraph("请框住最高的方块。寻路也可能改变框外方块。",layout.x(),compact?87:112,layout.width(),2,GOLD);
        int half=(layout.width()-6)/2;
        icon(button("开始 / 继续选点",layout.x(),compact?110:140,half,()->RegionPick.begin(minecraft,this)),Items.STICK);
        icon(tip(button("清除选区",layout.x()+half+6,compact?110:140,half,()->{RegionPick.clear();minecraft.gui.setScreen(new RegionScreen(parent));}),"只清除选点和框线，不会清除方块。"),Items.BARRIER);
        icon(button("检查范围，准备清除",layout.x(),compact?layout.footerY():166,compact?half:layout.width(),()->{
            RegionPick.validate(minecraft);BlockPos a=RegionPick.first(), b=RegionPick.second();
            if(a==null || b==null) {status="请先用木棍选择两个角。";return;}
            var volume=RegionVolume.withinLimit(a.getX(),a.getY(),a.getZ(),b.getX(),b.getY(),b.getZ());
            if(volume.isEmpty()) {status="范围超过 10 万格，请缩小选区。";return;}
            RegionPick.finish();minecraft.gui.setScreen(new ConfirmRegionScreen(this,a,b,volume.getAsLong(),ClientActions.connection(minecraft),ClientActions.dimension(minecraft)));
        }),Items.PAPER);
        icon(button("结束选点并返回",compact?layout.x()+half+6:layout.x(),layout.footerY(),compact?half:layout.width(),()->{RegionPick.finish();onClose();}),Items.ARROW);
    }
    @Override public void onClose() { RegionPick.finish();super.onClose(); }
    private String format(BlockPos p) {return p==null?"还没有选择":p.getX()+", "+p.getY()+", "+p.getZ();}
}

