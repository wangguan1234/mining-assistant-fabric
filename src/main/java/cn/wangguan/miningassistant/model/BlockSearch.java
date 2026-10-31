package cn.wangguan.miningassistant.model;

import com.ibm.icu.text.Transliterator;
import java.util.Locale;

/** Minecraft 26.2 already provides ICU; use it for Chinese pinyin without shipping another library. */
public final class BlockSearch {
    private static final ThreadLocal<Transliterator> PINYIN=ThreadLocal.withInitial(()->Transliterator.getInstance("Han-Latin; Latin-ASCII; Lower"));
    private BlockSearch() {}
    public static String pinyin(String label){return compact(PINYIN.get().transliterate(label));}
    public static boolean matches(String label,String id,String pinyin,String query){
        String needle=query.strip().toLowerCase(Locale.ROOT);
        if(needle.isEmpty())return true;
        if(label.toLowerCase(Locale.ROOT).contains(needle) || id.toLowerCase(Locale.ROOT).contains(needle))return true;
        String normalized=compact(needle);
        return !normalized.isEmpty() && normalized.matches("[a-z0-9]+") && pinyin.contains(normalized);
    }
    private static String compact(String value){return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    public static String hint(String id){
        if(id.equals("minecraft:diamond_ore") || id.equals("minecraft:deepslate_diamond_ore"))return "天然钻石矿石，地下生成；两种矿石要分别选择。";
        if(id.equals("minecraft:diamond_block"))return "钻石块是合成方块，不是地下的钻石矿石。";
        if(id.equals("minecraft:ancient_debris"))return "远古残骸在下界生成，可烧炼为下界合金碎片。";
        if(id.equals("minecraft:netherite_block"))return "下界合金块是合成方块，不是远古残骸。";
        String path=id.substring(id.indexOf(':')+1);
        if(path.startsWith("stripped_") && (path.endsWith("_log")||path.endsWith("_wood")||path.endsWith("_stem")||path.endsWith("_hyphae")))return "这是去皮后的木材，与自然生成的原木不同。";
        if(path.endsWith("_log")||path.endsWith("_stem"))return "原木是树干；只寻找这一种方块，不会自动砍完整棵树。";
        if(path.endsWith("_wood")||path.endsWith("_hyphae")||path.endsWith("_planks"))return "这是木头或木板，与树上自然生成的原木不同。";
        return "只会寻找你选择的这一种方块。";
    }
}

