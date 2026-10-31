package cn.wangguan.miningassistant;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerInput;

/** Uses normal client actions and temporarily yields a digging task when combat needs the hand. */
public final class ProtectionController {
    private record Threat(int entityId,UUID uuid,long expires) {}
    private static final Map<UUID,Threat> threats=new LinkedHashMap<>();
    private static Object connection,world;
    private static long ticks,nextFoodAttempt;
    private static boolean eating;
    private static ItemStack startedFood=ItemStack.EMPTY;
    private static FoodSwap foodSwap;
    private static LocalPlayer combatPlayer;
    private static ItemStack combatWeapon=ItemStack.EMPTY, displaced=ItemStack.EMPTY;
    private static int previousSlot=-1,weaponSlot=-1,backpackSlot=-1;
    private static long weaponReadyAt,lastNearTick;
    private static boolean fighting,pausedForDigging,manualChange,pauseWarning;
    private ProtectionController() {}
    private static void reset(){
        threats.clear();eating=false;startedFood=ItemStack.EMPTY;foodSwap=null;nextFoodAttempt=0;
        BaritoneTasks.releaseCombatPause();clearCombat();
    }
    private static void syncContext(Minecraft mc){
        Object nextConnection=ClientActions.connection(mc);
        if(connection!=nextConnection || world!=mc.level){reset();connection=nextConnection;world=mc.level;}
    }
    private static boolean available(Minecraft mc){
        return mc.player!=null && mc.level!=null && mc.gameMode!=null && mc.getConnection()!=null && mc.player.isAlive()
            && !mc.player.isSpectator() && mc.gui.screen()==null && !RegionPick.active()
            && MiningAssistantClient.protection!=null && MiningAssistantClient.protection.readable();
    }
    public static void onDamage(Minecraft mc,ClientboundDamageEventPacket packet){
        syncContext(mc);
        if(!available(mc) || !MiningAssistantClient.protection.retaliate() || packet.entityId()!=mc.player.getId())return;
        Entity attacker=packet.getSource(mc.level).getEntity();
        if(!(attacker instanceof LivingEntity) || !(attacker instanceof Enemy) || attacker instanceof Player || !attacker.isAlive())return;
        threats.remove(attacker.getUUID());
        threats.put(attacker.getUUID(),new Threat(attacker.getId(),attacker.getUUID(),ticks+120));
        while(threats.size()>4)threats.remove(threats.keySet().iterator().next());
    }
    public static void tick(Minecraft mc){
        ticks++;syncContext(mc);
        boolean working=available(mc),recovering=EmergencyController.allowFood(mc);
        if(!working && !recovering){
            stopEating(mc);finishCombat(mc);threats.clear();manualChange=false;return;
        }
        if(!working){finishCombat(mc);threats.clear();manualChange=false;}
        LocalPlayer player=mc.player;
        if(eating && (!player.isUsingItem() || player.getUsedItemHand()!=InteractionHand.OFF_HAND
            || !ItemStack.isSameItemSameComponents(startedFood,player.getOffhandItem()))) {
            finishFood(mc);nextFoodAttempt=ticks+5;
        }
        Entity target=working && MiningAssistantClient.protection.retaliate()?nearbyThreat(mc):null;
        if(!MiningAssistantClient.protection.retaliate())threats.clear();
        if(target!=null)lastNearTick=ticks;
        if(target==null && (!fighting || !pausedForDigging || ticks-lastNearTick>20 || !nearRememberedThreat(mc))) {
            finishCombat(mc);manualChange=false;
        }
        if(target!=null && !manualChange){
            if(eating && !mc.options.keyUse.isDown())stopEating(mc);
            boolean digging=mc.gameMode.getDestroyStage()>=0 || BaritoneTasks.breaking();
            if(digging || pausedForDigging){
                if(!BaritoneTasks.pauseForCombat()){
                    warnPause(player);
                    pausedForDigging=true;
                    return;
                }
                pausedForDigging=true;
                if(!BaritoneTasks.stopBreaking(mc.gameMode.getDestroyStage()>=0)){warnPause(player);return;}
                if(!BaritoneTasks.combatPauseReady() || mc.gameMode.getDestroyStage()>=0 || BaritoneTasks.breaking())return;
            }
            if(fighting && player.getInventory().getSelectedSlot()!=weaponSlot){
                finishCombat(mc);manualChange=true;return;
            }
            if(fighting && !combatWeapon.isEmpty() && !sameWeapon(combatWeapon,player.getMainHandItem())){
                boolean broken=player.getMainHandItem().isEmpty();
                finishCombat(mc);
                if(!broken){manualChange=true;return;}
            }
            if(!fighting)beginCombat(mc);
            // 没有剑或斧时保持停工，不能拿镐子、铲子等施工工具去挥怪物。
            if(fighting && !combatWeapon.isEmpty() && ticks>=weaponReadyAt && player.getAttackStrengthScale(0.0F)>=1.0F && !player.isUsingItem()){
                mc.gameMode.attack(player,target);
                player.swing(InteractionHand.MAIN_HAND);nextFoodAttempt=ticks+5;
            }
        }
        if(!MiningAssistantClient.protection.autoEat()) {stopEating(mc);return;}
        if(eating || player.isUsingItem() || target!=null || fighting || ticks<nextFoodAttempt || player.isCreative())return;
        if(player.getFoodData().getFoodLevel()>=20)return;
        int foodSlot=FoodSwap.preferredSlot(mc);
        if(foodSlot==-1)return;
        if(foodSlot>=0){
            foodSwap=FoodSwap.move(mc,foodSlot);
            if(foodSwap==null){nextFoodAttempt=ticks+40;return;}
        }
        ItemStack offhand=player.getOffhandItem();
        if(offhand.isEmpty() || !offhand.has(DataComponents.FOOD) || player.getCooldowns().isOnCooldown(offhand)){finishFood(mc);return;}
        ItemStack before=offhand.copy();nextFoodAttempt=ticks+20;
        mc.gameMode.useItem(player,InteractionHand.OFF_HAND);
        if(player.isUsingItem() && player.getUsedItemHand()==InteractionHand.OFF_HAND){eating=true;startedFood=before;}
        else finishFood(mc);
    }
    private static Entity nearbyThreat(Minecraft mc){
        Entity chosen=null;
        for(Iterator<Threat> it=threats.values().iterator();it.hasNext();){
            Threat threat=it.next();Entity entity=mc.level.getEntity(threat.entityId());
            if(threat.expires()<ticks || entity==null || !entity.getUUID().equals(threat.uuid()) || !entity.isAlive() || entity.isRemoved()) {it.remove();continue;}
            double distance=mc.player.distanceToSqr(entity);
            if(distance<=9.0 && (mc.player.hasLineOfSight(entity) || distance<=2.25))chosen=entity;
        }
        return chosen;
    }
    private static boolean nearRememberedThreat(Minecraft mc){
        for(Threat threat:threats.values()){
            Entity entity=mc.level.getEntity(threat.entityId());
            if(threat.expires()>=ticks && entity!=null && entity.getUUID().equals(threat.uuid()) && entity.isAlive()
                && mc.player.distanceToSqr(entity)<=25.0
                && (mc.player.hasLineOfSight(entity) || mc.player.distanceToSqr(entity)<=4.0))return true;
        }
        return false;
    }
    private static int findWeapon(LocalPlayer player){
        var inventory=player.getInventory();
        for(var tag:java.util.List.of(ItemTags.SWORDS,ItemTags.AXES))
            for(int i=0;i<36;i++)if(inventory.getItem(i).is(tag))return i;
        return -1;
    }
    private static void beginCombat(Minecraft mc){
        LocalPlayer player=mc.player;var inventory=player.getInventory();
        combatPlayer=player;previousSlot=inventory.getSelectedSlot();weaponSlot=previousSlot;backpackSlot=-1;
        int found=findWeapon(player);
        if(found>=9 && player.containerMenu==player.inventoryMenu && player.inventoryMenu.getCarried().isEmpty()){
            ItemStack weapon=inventory.getItem(found).copy();
            ItemStack before=inventory.getItem(previousSlot).copy();
            int sourceMenuSlot=player.inventoryMenu.findSlot(inventory,found).orElse(-1);
            int targetMenuSlot=player.inventoryMenu.findSlot(inventory,previousSlot).orElse(-1);
            if(sourceMenuSlot<0 || targetMenuSlot<0)return;
            mc.gameMode.handleContainerInput(player.inventoryMenu.containerId,sourceMenuSlot,targetMenuSlot,ContainerInput.SWAP,player);
            if(ItemStack.matches(weapon,inventory.getItem(previousSlot)) && ItemStack.matches(before,inventory.getItem(found))){
                backpackSlot=found;displaced=before;combatWeapon=weapon;
            }
        }else if(found>=0 && found<9){
            weaponSlot=found;combatWeapon=inventory.getItem(found).copy();inventory.setSelectedSlot(found);
        }
        if(found<0 || (found>=9 && backpackSlot<0)){
            combatWeapon=ItemStack.EMPTY;
            player.sendSystemMessage(Component.literal("[挖矿辅助] 没有找到剑或斧，已停止挖掘；不会拿镐子攻击。"));
        }
        fighting=true;weaponReadyAt=ticks+2;
    }
    private static void finishCombat(Minecraft mc){
        BaritoneTasks.releaseCombatPause();pausedForDigging=false;
        if(fighting && mc.player==combatPlayer && mc.player.isAlive() && mc.gameMode!=null){
            var inventory=mc.player.getInventory();
            if(backpackSlot>=0 && inventory.getSelectedSlot()==weaponSlot && mc.player.containerMenu==mc.player.inventoryMenu
                && mc.player.inventoryMenu.getCarried().isEmpty() && ItemStack.matches(displaced,inventory.getItem(backpackSlot))
                && (inventory.getItem(weaponSlot).isEmpty() || sameWeapon(combatWeapon,inventory.getItem(weaponSlot)))){
                int sourceMenuSlot=mc.player.inventoryMenu.findSlot(inventory,backpackSlot).orElse(-1);
                int targetMenuSlot=mc.player.inventoryMenu.findSlot(inventory,weaponSlot).orElse(-1);
                if(sourceMenuSlot>=0 && targetMenuSlot>=0)
                    mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,sourceMenuSlot,targetMenuSlot,ContainerInput.SWAP,mc.player);
            }else if(backpackSlot>=0){
                mc.player.sendSystemMessage(Component.literal("[挖矿辅助] 背包位置已变化，请检查剑、斧和工具的位置。"));
            }
            if(inventory.getSelectedSlot()==weaponSlot && previousSlot>=0)inventory.setSelectedSlot(previousSlot);
        }
        clearCombat();
    }
    private static void clearCombat(){
        combatPlayer=null;combatWeapon=ItemStack.EMPTY;displaced=ItemStack.EMPTY;
        previousSlot=-1;weaponSlot=-1;backpackSlot=-1;fighting=false;pausedForDigging=false;pauseWarning=false;weaponReadyAt=0;
    }
    private static void warnPause(LocalPlayer player){
        if(!pauseWarning)player.sendSystemMessage(Component.literal("[挖矿辅助] 无法安全暂停挖掘，请手动应对怪物。"));
        pauseWarning=true;
    }
    private static boolean sameWeapon(ItemStack before,ItemStack now){
        return !now.isEmpty() && ItemStack.matchesIgnoringComponents(before,now,type->type==DataComponents.DAMAGE);
    }
    private static void stopEating(Minecraft mc){
        if(eating && mc.gameMode!=null && mc.player!=null && mc.player.isUsingItem()
            && mc.player.getUsedItemHand()==InteractionHand.OFF_HAND && !mc.options.keyUse.isDown())mc.gameMode.releaseUsingItem(mc.player);
        finishFood(mc);
    }
    static void prepareForTeleport(Minecraft mc){
        stopEating(mc);finishCombat(mc);
    }
    private static void finishFood(Minecraft mc){
        eating=false;startedFood=ItemStack.EMPTY;
        if(foodSwap!=null){
            if(!foodSwap.restore(mc) && mc.player!=null && mc.player.isAlive())
                mc.player.sendSystemMessage(Component.literal("[挖矿辅助] 背包或副手已变化，请检查食物和原来副手的物品。"));
            foodSwap=null;
        }
    }
    /** Only prevent vanilla's release for a use that this controller started. */
    public static boolean holdFood(Minecraft mc,Player player){
        return eating && player==mc.player && player.isUsingItem() && player.getUsedItemHand()==InteractionHand.OFF_HAND
            && ItemStack.isSameItemSameComponents(startedFood,player.getOffhandItem()) && (available(mc) || EmergencyController.allowFood(mc))
            && MiningAssistantClient.protection.autoEat();
    }
}

