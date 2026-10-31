package cn.wangguan.miningassistant.model;

public record UiLayout(int x, int width, int rows, int formRows, int footerY) {
    public static UiLayout of(int width, int height) {
        int content = Math.min(720, Math.max(360, width - 32));
        return new UiLayout((width - content) / 2, content,
                Math.max(1, Math.min(8, (height - 150) / 30)),
                Math.max(1, Math.min(6, (height - 190) / 48)),
                Math.max(120, height - 36));
    }

    public static int clampPage(int page, int total, int pageSize) {
        int pages = Math.max(1, (total + Math.max(1, pageSize) - 1) / Math.max(1, pageSize));
        return Math.max(0, Math.min(page, pages - 1));
    }
}
