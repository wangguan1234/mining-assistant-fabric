package cn.wangguan.miningassistant;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Reads the player's own inventory; it never takes anything from containers. */
public final class FoodInventory {
    public record Food(String id,String name,int count,ItemStack icon) {}
    private FoodInventory() {}
    public static String id(ItemStack stack){
        return stack.isEmpty()?"":BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
    public static boolean edible(ItemStack stack){return !stack.isEmpty() && stack.has(DataComponents.FOOD);}
    public static Map<String,Food> available(Minecraft mc){
        Map<String,Food> result=new LinkedHashMap<>();
        if(mc.player==null)return result;
        for(int i=0;i<36;i++)add(result,mc.player.getInventory().getItem(i));
        add(result,mc.player.getOffhandItem());
        return result;
    }
    private static void add(Map<String,Food> result,ItemStack stack){
        if(!edible(stack))return;
        String id=id(stack);Food old=result.get(id);
        result.put(id,new Food(id,stack.getHoverName().getString(),stack.getCount()+(old==null?0:old.count()),stack.copyWithCount(1)));
    }
    public static String summary(Minecraft mc){
        if(MiningAssistantClient.protection==null || !MiningAssistantClient.protection.autoEat())return "自动吃东西已关闭。";
        var settings=MiningAssistantClient.foodPreferences;
        if(settings==null || !settings.readable())return "食物设置无法读取，自动吃东西已停用。";
        var available=available(mc);
        if(!settings.custom())return "背包有 "+available.size()+" 种食物；默认先吃副手，再按常见食物顺序吃背包里的食物。";
        var present=settings.order().stream().filter(available::containsKey).map(id->available.get(id).name()+" ×"+available.get(id).count()).toList();
        return present.isEmpty()?"背包里没有允许自动吃的食物。":"按顺序吃："+String.join(" → ",present);
    }
}

