package cn.wangguan.miningassistant.ui;

import net.minecraft.client.gui.screens.Screen;

final class RecoveryTipScreen extends HelperScreen {
    RecoveryTipScreen(Screen parent){super("回血小贴士",parent);}
    @Override protected void init(){
        super.init();
        paragraph("可以在家设计压力板和发射器，踩上压力板后喷出治疗药水。先自己测试机关，再站在希望落脚的位置保存家。模组不会自动建造机关。",layout.x(),40,layout.width(),6,MUTED);
        button("返回上一页",layout.x(),layout.footerY(),layout.width(),this::onClose);
    }
}

