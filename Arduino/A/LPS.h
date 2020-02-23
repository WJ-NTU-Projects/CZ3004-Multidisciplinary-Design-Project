#pragma once
#include <Arduino.h>

#define WHEEL_AXIS 179

class LPS {
    public:
        LPS(int *tl, int *tr, float tpmm);
        void computePosition();
        void reset();
        float getX();
        float getY();
        int getHeading();

    private:
        int *ticksLeft;
        int *ticksRight;
        float ticksPerMillimeter;
        float x;
        float y;
        float headingRadian;
        int headingDegree;
        float deltaLeft;
        float deltaRight;
        int boundAngle(int angle);
};
