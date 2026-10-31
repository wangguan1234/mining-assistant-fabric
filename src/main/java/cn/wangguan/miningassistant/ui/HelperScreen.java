package cn.wangguan.miningassistant.ui;

import cn.wangguan.miningassistant.model.UiLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import java.util.ArrayList;
import java.util.List;

public abstract class HelperScreen extends Screen {
    protected static final int INK = 0xFFE5E3DA, MUTED = 0xFFB4BBB9, GOLD = 0xFFFFD182, RED = 0xFFFFA69A;
    protected final Screen parent;
    protected UiLayout layout;
    protected String status = "";
    private record Icon(Button button,ItemStack item) {}
    private final List<Icon> icons=new ArrayList<>();

    protected HelperScreen(String title, Screen parent) { super(Component.literal(title)); this.parent = parent; }
    @Override protected void init() { layout = UiLayout.of(width, height); icons.clear(); }
    protected Button button(String text, int x, int y, int width, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(text), ignored -> action.run()).bounds(x,y,width,20).build());
    }
    protected Button tip(Button button, String text) { button.setTooltip(Tooltip.create(Component.literal(text))); return button; }
    protected Button icon(Button button,ItemLike item){return icon(button,iconStack(item));}
    protected Button icon(Button button,ItemStack item){if(item!=null&&!item.isEmpty())icons.add(new Icon(button,item));return button;}
    protected static ItemStack blockIcon(String id){
        Identifier key=Identifier.tryParse(id);
        if(key==null)return ItemStack.EMPTY;
        var block=BuiltInRegistries.BLOCK.getValue(key);
        return block==null?ItemStack.EMPTY:iconStack(block);
    }
    private static ItemStack iconStack(ItemLike item){
        try{return new ItemStack(item);}catch(NullPointerException e){return ItemStack.EMPTY;}
    }
    protected void paragraph(String text, int x, int y, int width, int lines, int color) {
        var label = new MultiLineTextWidget(x,y,Component.literal(text).withStyle(style -> style.withColor(color & 0xFFFFFF)),font).setMaxWidth(width).setMaxRows(lines).setCentered(false);
        addRenderableOnly(label);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        boolean handled = super.mouseClicked(event, doubled);
        // The vanilla container assigns focus after a button callback may rebuild the page.
        if (getFocused() != null && !children().contains(getFocused())) {
            clearFocus();
            setDragging(false);
        }
        return handled;
    }
    @Override public boolean isPauseScreen() { return true; }
    protected int statusY() { return layout.footerY()-13; }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        for(var icon:icons){
            Button button=icon.button();
            int textWidth=font.width(button.getMessage());
            if(button.visible && button.active && button.getWidth()>=textWidth+42){
                int iconX=button.getX()+(button.getWidth()-textWidth)/2-20;
                graphics.fakeItem(icon.item(),iconX,button.getY()+2);
            }
        }
        graphics.text(font, title, layout.x(), 10, GOLD, true);
        if (!status.isEmpty()) graphics.text(font, font.plainSubstrByWidth(status, layout.width()), layout.x(), statusY(), RED, false);
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0,0,width,height,0xF0161D20);
        graphics.fill(0,0,width,31,0xFF232E32);
    }
}

