#include "LPS.h"

LPS::LPS(double *tl, double *tr, double tpmm) {
    ticksLeft = tl;
    ticksRight = tr;
    ticksPerMillimeter = tpmm;
    reset();
}

void LPS::reset() {
    x = 0;
    y = 0;
    previousTicksLeft = 0;
    previousTicksRight = 0;
    headingRadian = 0;
    headingDegree = 0;
    deltaLeft = 0;
    deltaRight = 0;
}

void LPS::computePosition() {
    double currentTicksLeft = *ticksLeft;
    double currentTicksRight = *ticksRight;
    deltaLeft = (currentTicksLeft - previousTicksLeft) * ticksPerMillimeter;
    deltaRight = (currentTicksRight - previousTicksRight) * ticksPerMillimeter;
    double deltaMean = (deltaLeft + deltaRight) * 0.5;
    double diff = (deltaRight - deltaLeft) * WHEEL_AXIS_MULTIPLIER;
    headingRadian += diff;
    x += deltaMean * cos(headingRadian);
    y += deltaMean * sin(headingRadian); 
    previousTicksLeft = currentTicksLeft;
    previousTicksRight = currentTicksRight;
}

void LPS::computeLeftTurn() {
    double currentTicksLeft = *ticksLeft;
    double currentTicksRight = *ticksRight;
    deltaLeft = (currentTicksLeft - previousTicksLeft) * ticksPerMillimeter;
    deltaRight = (currentTicksRight - previousTicksRight) * ticksPerMillimeter;
    double diff = (deltaRight - 0) * WHEEL_AXIS_HALF_MULTIPLIER;
    y = deltaRight - deltaLeft;
    headingRadian += diff; 
    headingDegree = round((headingRadian * 4068) * 0.0140845);
    previousTicksLeft = currentTicksLeft;
    previousTicksRight = currentTicksRight;
}

void LPS::computeRightTurn() {
    double currentTicksLeft = *ticksLeft;
    double currentTicksRight = *ticksRight;
    deltaLeft = (currentTicksLeft - previousTicksLeft) * ticksPerMillimeter;
    deltaRight = (currentTicksRight - previousTicksRight) * ticksPerMillimeter;
    double diff = (deltaLeft - 0) * WHEEL_AXIS_HALF_MULTIPLIER;
    y = deltaRight - deltaLeft;
    headingRadian += diff;  
    headingDegree = round((headingRadian * 4068) * 0.0140845);
    previousTicksLeft = currentTicksLeft;
    previousTicksRight = currentTicksRight;
}

double LPS::getX() {
    return x;
}

double LPS::getY() {
    return y;
}

int LPS::getHeading() {
    return headingDegree;
}
