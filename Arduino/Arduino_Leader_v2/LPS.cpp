#include "LPS.h"

LPS::LPS(double *tl, double *tr, double tpmm) {
    ticksLeft = tl;
    ticksRight = tr;
    ticksPerMillimeter = tpmm * 1.0;
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
    double ticksDiffLeft = currentTicksLeft - previousTicksLeft;
    double ticksDiffRight = currentTicksRight - previousTicksRight;
    deltaLeft = ticksDiffLeft / ticksPerMillimeter;
    deltaRight = ticksDiffRight / ticksPerMillimeter;
    double deltaMean = (deltaLeft + deltaRight) * 0.5;
    double diff = (deltaRight - deltaLeft) / WHEEL_AXIS;
    headingRadian += diff;
    double circle = 2 * PI;
    if (headingRadian >= circle) headingRadian -= circle;      
    if (headingRadian <= -circle) headingRadian += circle;     
    x += deltaMean * cos(headingRadian);
    y += deltaMean * sin(headingRadian); 
    previousTicksLeft = currentTicksLeft;
    previousTicksRight = currentTicksRight;
}

void LPS::computeLeftTurn() {
    double currentTicksLeft = *ticksLeft;
    double currentTicksRight = *ticksRight;
    double ticksDiffLeft = currentTicksLeft - previousTicksLeft;
    double ticksDiffRight = currentTicksRight - previousTicksRight;
    deltaLeft = ticksDiffLeft / ticksPerMillimeter;
    deltaRight = ticksDiffRight / ticksPerMillimeter;
    double diff = (deltaRight - 0) / (WHEEL_AXIS * 0.5);
    y = deltaRight - deltaLeft;
    headingRadian += diff;
    double circle = 2 * PI;
    if (headingRadian >= circle) headingRadian -= circle;      
    if (headingRadian <= -circle) headingRadian += circle;     
    headingDegree = round((headingRadian * 4068) / 71.0);
    previousTicksLeft = currentTicksLeft;
    previousTicksRight = currentTicksRight;
}

void LPS::computeRightTurn() {
    double currentTicksLeft = *ticksLeft;
    double currentTicksRight = *ticksRight;
    double ticksDiffLeft = currentTicksLeft - previousTicksLeft;
    double ticksDiffRight = currentTicksRight - previousTicksRight;
    deltaLeft = ticksDiffLeft / ticksPerMillimeter;
    deltaRight = ticksDiffRight / ticksPerMillimeter;
    double diff = (deltaLeft - 0) / (WHEEL_AXIS * 0.5);
    y = deltaRight - deltaLeft;
    headingRadian += diff;
    double circle = 2 * PI;
    if (headingRadian >= circle) headingRadian -= circle;      
    if (headingRadian <= -circle) headingRadian += circle;     
    headingDegree = round((headingRadian * 4068) / 71.0);
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

int LPS::boundAngle(int angle) {
    if (angle >= 360) angle -= 360;
    if (angle <= -360) angle += 360;
    return angle; 
}
