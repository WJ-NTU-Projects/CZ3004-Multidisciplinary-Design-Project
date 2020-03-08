#include "LPS.h"

LPS::LPS(double *tl, double *tr, double tpmm) {
    ticksLeft = tl;
    ticksRight = tr;
    ticksPerMillimeter = tpmm;
    reset();
}

void LPS::reset() {
    previousTicksLeft = 0;
    previousTicksRight = 0;
    headingRadian = 0;
    error = 0;
}

double LPS::computeError() {
    double currentTicksLeft = *ticksLeft;
    double currentTicksRight = *ticksRight;
    double deltaLeft = (currentTicksLeft - previousTicksLeft) / ticksPerMillimeter;
    double deltaRight = (currentTicksRight - previousTicksRight) / ticksPerMillimeter;
    double deltaMean = (deltaLeft + deltaRight) * 0.5;
    double diff = (deltaRight - deltaLeft) / WHEEL_AXIS;
    headingRadian += diff;
    error += deltaMean * sin(headingRadian); 
    previousTicksLeft = currentTicksLeft;
    previousTicksRight = currentTicksRight;
    return error;
}
