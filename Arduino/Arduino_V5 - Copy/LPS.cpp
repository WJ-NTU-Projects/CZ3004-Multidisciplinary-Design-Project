#include "LPS.h"

LPS::LPS(double *tl, double *tr, double tpmm) {
    ticksLeft = tl;
    ticksRight = tr;
    ticksPerMillimeter = tpmm;
    reset();
}

void LPS::reset() {
    error = 0;
    previousTicksLeft = 0;
    previousTicksRight = 0;
    headingRadian = 0;
    deltaLeft = 0;
    deltaRight = 0;
}

double LPS::computeError() {
    double currentTicksLeft = *ticksLeft;
    double currentTicksRight = *ticksRight;
    deltaLeft = (currentTicksLeft - previousTicksLeft) / ticksPerMillimeter;
    deltaRight = (currentTicksRight - previousTicksRight) / ticksPerMillimeter;
    double deltaMean = (deltaLeft + deltaRight) * 0.5;
    double diff = (deltaRight - deltaLeft) / WHEEL_AXIS;
    headingRadian += diff;
    error += deltaMean * sin(headingRadian); 
    previousTicksLeft = currentTicksLeft;
    previousTicksRight = currentTicksRight;
    return error;
}
