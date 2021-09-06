package com.usim.engine.engine;

public interface Constants {
    int NANO = 1_000_000_000;
    int MILLION = 1_000_000;
    int MILLI = 1_000;
    int MEGA = 1_024 * 1_024;

    int DEFAULT_TARGET_FPS = 60;
    int DEFAULT_TARGET_UPS = 60;
    int DEFAULT_TARGET_IPS = 30;

    int DEFAULT_GLFW_WINDOW_WIDTH = 1280;
    int DEFAULT_GLFW_WINDOW_HEIGHT = 720;

    int GL_LOG_MAX_LENGTH = 1024;

    String DEFAULT_GLFW_WINDOW_NAME = "AHD:: GLFW Window";

    String DEFAULT_GLFW_ICON_PATH = ".\\src\\main\\resources\\icons\\usim-icon.png";
    String DEFAULT_SWING_ICON_PATH = ".\\src\\main\\resources\\icons\\usim-icon.png";

    float DEFAULT_FIELD_OF_VIEW = (float) Math.PI / 3;
    float DEFAULT_Z_NEAR = 0.01f;
    float DEFAULT_Z_FAR = 1000.f;

    float DEFAULT_MOUSE_MOVEMENT_SENSITIVITY = 0.002f;
    float DEFAULT_CAMERA_MOVEMENT_SENSITIVITY = 0.05f;
}
