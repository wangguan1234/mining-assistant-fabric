package cn.wangguan.miningassistant.model;

import java.util.OptionalLong;

/** Calculate an inclusive volume without overflowing the configured limit. */
public final class RegionVolume {
    public static final long LIMIT = 100_000;
    private RegionVolume() {}
    public static OptionalLong withinLimit(int x1,int y1,int z1,int x2,int y2,int z2) {
        long x=Math.abs((long)x1-x2)+1, y=Math.abs((long)y1-y2)+1, z=Math.abs((long)z1-z2)+1;
        if(x>LIMIT || y>LIMIT/x) return OptionalLong.empty();
        long xy=x*y;
        if(z>LIMIT/xy) return OptionalLong.empty();
        return OptionalLong.of(xy*z);
    }
}

