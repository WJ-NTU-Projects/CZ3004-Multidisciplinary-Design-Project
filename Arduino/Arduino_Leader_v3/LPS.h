#pragma once
#include <Arduino.h>

#define WHEEL_AXIS 179

class LPS {
    public:
        LPS(double *tl, double *tr, double tpmm);
        void computePosition();
        void computeLeftTurn();
        void computeRightTurn();
        void reset();
        double getX();
        double getY();
        int getHeading();

    private:
        double *ticksLeft;
        double *ticksRight;
        double previousTicksLeft;
        double previousTicksRight;
        double ticksPerMillimeter;
        double x;
        double y;
        double headingRadian;
        int headingDegree;
        double deltaLeft;
        double deltaRight;
        int boundAngle(int angle);
};
