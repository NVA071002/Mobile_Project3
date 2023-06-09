package com.example.project3.Common;

public class FrameMetadata {
    private final int width;
    private final int height;
    private final int rotation;
    private final int cameraFacing;
    public static final int UNKNOWN_CAMERA_FACING = -1;
    private FrameMetadata(int width, int height, int rotation, int cameraFacing) {
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.cameraFacing = cameraFacing;
    }
    public int getCameraFacing() {
        return cameraFacing;
    }

    public int getHeight() {
        return height;
    }

    public int getRotation() {
        return rotation;
    }

    public int getWidth() {
        return width;
    }

    public static class Builder {
        private int width = 0;
        private int height = 0;
        private int rotation = 0;
        private int cameraFacing = 0;

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setRotation(int rotation) {
            this.rotation = rotation;
            return this;
        }

        public Builder setCameraFacing(int cameraFacing) {
            this.cameraFacing = cameraFacing;
            return this;
        }


        public FrameMetadata build() {
            return new FrameMetadata(width, height, rotation, cameraFacing);
        }
    }

}
