package cn.wangguan.miningassistant;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/** Temporarily moves a chosen food to the offhand through a normal inventory action. */
final class FoodSwap {
    private static final java.util.List<String> DEFAULT_ORDER=java.util.List.of(
        "minecraft:golden_carrot","minecraft:cooked_beef","minecraft:cooked_porkchop",
        "minecraft:cooked_mutton","minecraft:cooked_chicken","minecraft:cooked_rabbit",
        "minecraft:cooked_cod","minecraft:cooked_salmon","minecraft:baked_potato",
        "minecraft:bread","minecraft:apple","minecraft:carrot","minecraft:potato",
        "minecraft:beetroot","minecraft:melon_slice","minecraft:sweet_berries","minecraft:dried_kelp");
    private final Object connection,world;
    private final LocalPlayer player;
    private final int inventoryIndex,menuSlot;
    private final ItemStack food,oldOffhand;
    private FoodSwap(Minecraft mc,int inventoryIndex,int menuSlot,ItemStack food,ItemStack oldOffhand){
        connection=ClientActions.connection(mc);world=mc.level;player=mc.player;
        this.inventoryIndex=inventoryIndex;this.menuSlot=menuSlot;this.food=food;this.oldOffhand=oldOffhand;
    }
    static int preferredSlot(Minecraft mc){
        var prefs=MiningAssistantClient.foodPreferences;
        if(prefs==null || !prefs.readable())return -1;
        if(!prefs.custom()){
            if(FoodInventory.edible(mc.player.getOffhandItem()))return -2;
            for(String id:DEFAULT_ORDER)for(int i=0;i<36;i++){
                ItemStack stack=mc.player.getInventory().getItem(i);
                if(FoodInventory.edible(stack) && FoodInventory.id(stack).equals(id))return i;
            }
            for(int i=0;i<36;i++)if(FoodInventory.edible(mc.player.getInventory().getItem(i)))return i;
            return -1;
        }
        for(String id:prefs.order()){
            if(FoodInventory.edible(mc.player.getOffhandItem()) && FoodInventory.id(mc.player.getOffhandItem()).equals(id))return -2;
            for(int i=0;i<36;i++){
                ItemStack stack=mc.player.getInventory().getItem(i);
                if(FoodInventory.edible(stack) && FoodInventory.id(stack).equals(id))return i;
            }
        }
        return -1;
    }
    static FoodSwap move(Minecraft mc,int index){
        LocalPlayer p=mc.player;
        if(index<0 || index>=36 || p.containerMenu!=p.inventoryMenu || !p.inventoryMenu.getCarried().isEmpty())return null;
        int slot=p.inventoryMenu.findSlot(p.getInventory(),index).orElse(-1);
        if(slot<0)return null;
        ItemStack food=p.getInventory().getItem(index).copy(),old=p.getOffhandItem().copy();
        if(!FoodInventory.edible(food))return null;
        mc.gameMode.handleContainerInput(p.inventoryMenu.containerId,slot,40,ContainerInput.SWAP,p);
        if(!ItemStack.matches(food,p.getOffhandItem()) || !ItemStack.matches(old,p.getInventory().getItem(index)))return null;
        return new FoodSwap(mc,index,slot,food,old);
    }
    boolean restore(Minecraft mc){
        if(mc.player!=player || ClientActions.connection(mc)!=connection || mc.level!=world || mc.gameMode==null || !player.isAlive()
            || player.containerMenu!=player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty())return false;
        ItemStack current=player.getOffhandItem(),source=player.getInventory().getItem(inventoryIndex);
        if(!ItemStack.matches(oldOffhand,source))return false;
        if(!current.isEmpty() && (!ItemStack.isSameItemSameComponents(food,current) || current.getCount()>food.getCount()))return false;
        mc.gameMode.handleContainerInput(player.inventoryMenu.containerId,menuSlot,40,ContainerInput.SWAP,player);
        return ItemStack.matches(oldOffhand,player.getOffhandItem());
    }
}

