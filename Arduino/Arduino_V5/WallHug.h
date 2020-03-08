#pragma once
#include <Arduino.h>

class WallHug {
    public:
        WallHug();
        void init();
        void move(int x, int y);
        void moveForward();
        void turn(int angle);
        void setExplored(int x, int y);
        void setObstacle(int x, int y);
        bool isFrontMovable();
        bool isLeftMovable();
        bool isBackAtStart();
        void updateSensor1(int reading);
        void updateSensor2(int reading);
        void updateSensor3(int reading);
        void updateSensor4(int reading);
        void updateSensor5(int reading);

    private:
        int arena[20][15];
        int position[2];
        int facing;
        bool isObstacle(int x, int y);
        bool isMovable(int x, int y);
        bool isInvalidCoordinates(int x, int y, bool robotSize);
};
