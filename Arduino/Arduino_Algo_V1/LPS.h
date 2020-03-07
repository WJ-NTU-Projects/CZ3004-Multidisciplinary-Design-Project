#pragma once
#include <Arduino.h>

#define WHEEL_AXIS 179
#define WHEEL_AXIS_MULTIPLIER 0.0055866
#define WHEEL_AXIS_HALF_MULTIPLIER 0.0111732

class LPS {
    public:
        LPS(double *tl, double *tr, double tpmm);
        double computeError();
        void reset();

    private:
        double *ticksLeft;
        double *ticksRight;
        double previousTicksLeft;
        double previousTicksRight;
        double ticksPerMillimeter;
        double error;
        double headingRadian;
        double deltaLeft;
        double deltaRight;
};
