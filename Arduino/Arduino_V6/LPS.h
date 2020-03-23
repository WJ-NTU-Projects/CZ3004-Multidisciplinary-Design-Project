#pragma once
#include <Arduino.h>

#define WHEEL_AXIS 179

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
