package cn.wangguan.miningassistant.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/** Builder settings are global to Baritone and can alter a clear/tunnel operation. */
final class BuilderSafety {
    private record Rule(String name,String description,Object safe) {}
    record Inspection(boolean available,List<String> names,List<String> descriptions) {
        boolean safe(){return available && names.isEmpty();}
    }
    private static final List<Rule> RULES=List.of(
        new Rule("buildRepeat","重复施工",new Vec3i(0,0,0)),
        new Rule("schematicOrientationX","施工方向",false),new Rule("schematicOrientationY","施工方向",false),
        new Rule("schematicOrientationZ","施工方向",false),new Rule("buildSchematicRotation","旋转",Rotation.NONE),
        new Rule("buildSchematicMirror","镜像",Mirror.NONE),new Rule("buildSubstitutes","方块替换",Map.of()),
        new Rule("buildSkipBlocks","跳过方块",List.of()),new Rule("buildIgnoreExisting","忽略已有方块",false),
        new Rule("buildIgnoreBlocks","忽略方块",List.of()),new Rule("buildValidSubstitutes","方块替换",Map.of()),
        new Rule("okIfWater","保留水",false),new Rule("buildInLayers","分层施工",false),
        new Rule("buildOnlySelection","限定施工范围",false),new Rule("mapArtMode","地图画模式",false)
    );
    private BuilderSafety() {}
    private static Object settings() throws ReflectiveOperationException {
        return Class.forName("baritone.api.BaritoneAPI").getMethod("getSettings").invoke(null);
    }
    private static Object entry(Object settings,String key) throws ReflectiveOperationException {
        return Class.forName("baritone.api.Settings").getField(key).get(settings);
    }
    static Inspection inspect() {
        try {
            Object settings=settings();var value=Class.forName("baritone.api.Settings$Setting").getField("value");
            List<String> names=new ArrayList<>(),descriptions=new ArrayList<>();
            for(Rule rule:RULES) {
                Object current=value.get(entry(settings,rule.name()));
                if(current==null || !rule.safe().getClass().isInstance(current) && !(rule.safe() instanceof Map && current instanceof Map) && !(rule.safe() instanceof List && current instanceof List))
                    return new Inspection(false,List.of(),List.of());
                if(!Objects.equals(current,rule.safe())) {names.add(rule.name());if(!descriptions.contains(rule.description()))descriptions.add(rule.description());}
            }
            return new Inspection(true,List.copyOf(names),List.copyOf(descriptions));
        } catch(ReflectiveOperationException | LinkageError | RuntimeException e){return new Inspection(false,List.of(),List.of());}
    }
    static boolean reset(List<String> names) {
        try {
            Object settings=settings();var value=Class.forName("baritone.api.Settings$Setting").getField("value");
            for(Rule rule:RULES)if(names.contains(rule.name())) {
                Object safe=rule.safe() instanceof Map?new HashMap<>():rule.safe() instanceof List?new ArrayList<>():rule.safe();
                value.set(entry(settings,rule.name()),safe);
            }
            return inspect().safe();
        } catch(ReflectiveOperationException | LinkageError | RuntimeException e){return false;}
    }
    static boolean ready(net.minecraft.client.Minecraft mc,net.minecraft.client.gui.screens.Screen parent) {
        var report=inspect();
        if(report.safe())return true;
        mc.gui.setScreen(new BuilderSafetyScreen(parent,report));return false;
    }
}

