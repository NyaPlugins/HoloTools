package net.kokoricraft.holotools.enums;

public enum HoloSize {
    SLOT_4(4),
    SLOT_5(5),
    SLOT_6(6),
    SLOT_7(7),
    SLOT_8(8),
    SLOT_9(9),
    SLOT_10(10),
    SLOT_11(11),
    SLOT_12(12),
    SLOT_13(13),
    SLOT_14(14),
    SLOT_15(15),
    SLOT_16(16);

    public static final float BASE_WIDTH = 0.125f;
    public static final float BASE_HEIGHT = 0.25f;
    public static final float INTERNAL_X_OFFSET = 0.0125f;
    public static final double DEFAULT_RADIUS_SMALL = -1.8;
    public static final double DEFAULT_RADIUS = -2.1;
    public static final float DEFAULT_HEIGHT = 2.0f;
    public static final float OVERLAP_FACTOR = 1.003f;

    private final double x;
    private final double z;
    private final float x_size;
    private final float y_size;
    private final float z_size;

    HoloSize(int slots) {
        double radius = calculateRadius(slots);
        float scaleX = calculateScaleX(slots, radius);
        this.x = calculateOffsetX(scaleX);
        this.z = radius;
        this.x_size = scaleX;
        this.y_size = calculateScaleY(DEFAULT_HEIGHT);
        this.z_size = 10.0f;
    }

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

    public static double calculateRadius(int slots) {
        return slots <= 4 ? DEFAULT_RADIUS_SMALL : DEFAULT_RADIUS;
    }

    public static double calculateWidth(int slots, double radius) {
        return calculateWidth(slots, radius, OVERLAP_FACTOR);
    }

    public static double calculateWidth(int slots, double radius, float overlapFactor) {
        return 2.0 * Math.abs(radius) * Math.tan(Math.PI / slots) * overlapFactor;
    }

    public static float calculateScaleX(int slots, double radius) {
        return calculateScaleX(slots, radius, OVERLAP_FACTOR);
    }

    public static float calculateScaleX(int slots, double radius, float overlapFactor) {
        double width = calculateWidth(slots, radius, overlapFactor);
        return (float) (width / BASE_WIDTH);
    }

    public static double calculateOffsetX(float scaleX) {
        return -INTERNAL_X_OFFSET * scaleX;
    }

    public static float calculateScaleY(float heightBlocks) {
        return heightBlocks / BASE_HEIGHT;
    }

    public static float calculateOffsetY(float targetCenterY, float scaleY) {
        return targetCenterY - (scaleY * BASE_HEIGHT) / 2.0f;
    }

    public static float[] calculateCenteredTranslation(float targetX, float targetY, float targetZ, float width, float height) {
        float scaleX = width / BASE_WIDTH;
        float scaleY = height / BASE_HEIGHT;
        float tx = (float) (targetX + calculateOffsetX(scaleX));
        float ty = calculateOffsetY(targetY, scaleY);
        return new float[]{tx, ty, targetZ};
    }

    public static class HoloSizeData {
        private final double x;
        private final double z;
        private final float x_size;
        private final float y_size;
        private final float z_size;

        public HoloSizeData(double x, double z, float x_size, float y_size, float z_size) {
            this.x = x;
            this.z = z;
            this.x_size = x_size;
            this.y_size = y_size;
            this.z_size = z_size;
        }

        public double getX() { return x; }
        public double getZ() { return z; }
        public float getXSize() { return x_size; }
        public float getYSize() { return y_size; }
        public float getZSize() { return z_size; }
    }

    public static HoloSizeData compute(int slots) {
        return compute(slots, calculateRadius(slots), DEFAULT_HEIGHT);
    }

    public static HoloSizeData compute(int slots, double radius, float heightBlocks) {
        float scaleX = calculateScaleX(slots, radius);
        float scaleY = calculateScaleY(heightBlocks);
        double offsetX = calculateOffsetX(scaleX);
        return new HoloSizeData(offsetX, radius, scaleX, scaleY, 10.0f);
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
