package dev.vitorsilverio.gbcemu.ppu;

public class PpuControl {

    private boolean enabled;
    private TileMapArea windowTileMapArea;
    private boolean windowEnabled;
    private TileArea tileArea;
    private TileMapArea bgTileMapArea;
    private byte spriteSize;
    private boolean spriteEnabled;
    private boolean BgOrWindowpriority;

    public PpuControl() {
        this.setData((byte) 0x00);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public TileMapArea getWindowTileArea() {
        return windowTileMapArea;
    }

    public boolean isWindowEnabled() {
        return windowEnabled;
    }


    public TileArea getTileArea() {
        return tileArea;
    }

    public TileMapArea getBgTileArea() {
        return bgTileMapArea;
    }

    public byte getSpriteSize() {
        return spriteSize;
    }

    public boolean isSpriteEnabled() {
        return spriteEnabled;
    }

    public boolean isBgOrWindowPriority() {
        return BgOrWindowpriority;
    }

    public byte getData() {
        byte data = 0;
        data |= (byte) (enabled ? 0x80 : 0);
        data |= (byte) (windowTileMapArea.getValue() << 6);
        data |= (byte) (windowEnabled ? 0x40 : 0);
        data |= (byte) (tileArea.getValue() << 4);
        data |= (byte) (bgTileMapArea.getValue() << 3);
        data |= (byte) (spriteSize << 2);
        data |= (byte) (spriteEnabled ? 0x02 : 0);
        data |= (byte) (BgOrWindowpriority ? 0x01 : 0);
        return data;
    }

    public void setData(byte value){
        this.enabled = (value & 0x80) != 0;
        this.windowTileMapArea = TileMapArea.fromValue((value >> 6) & 0x01);
        this.windowEnabled = (value & 0x40) != 0;
        this.tileArea = TileArea.fromValue((value >> 4) & 0x01);
        this.bgTileMapArea = TileMapArea.fromValue((value >> 3) & 0x01);
        this.spriteSize = (byte) ((value >> 2) & 0x01);
        this.spriteEnabled = (value & 0x02) != 0;
        this.BgOrWindowpriority = (value & 0x01) != 0;
    }

    @Override
    public String toString() {
        return "PpuControl{" +
                "enabled=" + enabled +
                ", windowTileMapArea=" + windowTileMapArea +
                ", windowEnabled=" + windowEnabled +
                ", tileArea=" + tileArea +
                ", bgTileMapArea=" + bgTileMapArea +
                ", spriteSize=" + spriteSize +
                ", spriteEnabled=" + spriteEnabled +
                ", BgOrWindowpriority=" + BgOrWindowpriority +
                '}';
    }
}
