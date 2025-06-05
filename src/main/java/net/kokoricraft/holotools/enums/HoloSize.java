package net.kokoricraft.holotools.enums;

public enum HoloSize {
    SLOT_4(-0.35, -1.8, 28.9f, 8, 10),
    SLOT_5(-0.245, -2.1, 24.9f, 8, 10),
    SLOT_6(-0.175, -2.1, 19.58f, 8, 10),
    SLOT_7(-0.175, -2.1, 16.38f, 8, 10),
    SLOT_8(-0.175, -2.1, 13.94f, 8, 10),
    SLOT_9(-0.175, -2.1, 12.48f, 8, 10),
    SLOT_10(-0.175, -2.1, 11.09f, 8, 10),
    SLOT_11(-0.175, -2.1, 10.1f, 8, 10),
    SLOT_12(-0.175, -2.1, 9.29f, 8, 10),
    SLOT_13(-0.175, -2.1, 8.42f, 8, 10),
    SLOT_14(-0.175, -2.1, 8.1f, 8, 10),
    SLOT_15(-0.175, -2.1, 7.65f, 8, 10),
    SLOT_16(-0.175, -2.1, 6.9f, 8, 10);//1.7425

    private final double x;
    private final double z;
    private final float x_size;
    private final float y_size;
    private final float z_size;

    HoloSize(double x, double z, float x_size, float y_size, float z_size) {
        this.x = x;
        this.z = z;
        this.x_size = x_size;
        this.y_size = y_size;
        this.z_size = z_size;
    }

    public double getZ() {
        return z;
    }

    public double getX() {
        return x;
    }

    public float getXSize() {
        return x_size;
    }

    public float getYSize() {
        return y_size;
    }

    public float getZSize() {
        return z_size;
    }

    public static HoloSize getBySlots(int slots) {
        if (slots <= 4)
            return HoloSize.SLOT_4;

        if (slots >= 16)
            return HoloSize.SLOT_16;

        return switch (slots) {
            case 5 -> HoloSize.SLOT_5;
            case 6 -> HoloSize.SLOT_6;
            case 7 -> HoloSize.SLOT_7;
            case 9 -> HoloSize.SLOT_9;
            case 10 -> HoloSize.SLOT_10;
            case 11 -> HoloSize.SLOT_11;
            case 12 -> HoloSize.SLOT_12;
            case 13 -> HoloSize.SLOT_13;
            case 14 -> HoloSize.SLOT_14;
            case 15 -> HoloSize.SLOT_15;
            default -> HoloSize.SLOT_8;
        };
    }
}
