#include "LPS.h"

LPS::LPS(int *tl, int *tr, float tpmm) {
    ticksLeft = tl;
    ticksRight = tr;
    ticksPerMillimeter = tpmm * 1.0;
    reset();
}

void LPS::reset() {
    x = 0;
    y = 0;
    headingRadian = 0;
    headingDegree = 0;
    deltaLeft = 0;
    deltaRight = 0;
}

void LPS::computePosition() {
    deltaLeft = *ticksLeft / ticksPerMillimeter;
    deltaRight = *ticksRight / ticksPerMillimeter;

    if (fabs(deltaLeft - deltaRight) < 1.0e-6) {
        x += deltaLeft * cos(headingRadian);
        y += deltaRight * sin(headingRadian);
        return;
    }

    float radius = WHEEL_AXIS * (deltaLeft + deltaRight) / (2 * (deltaRight - deltaLeft));
    float diff = (deltaRight - deltaLeft) / WHEEL_AXIS;
    x = x + (radius * sin(headingRadian + diff) - radius * sin(headingRadian));
    y = y - (radius * cos(headingRadian + diff) + radius * cos(headingRadian));
    headingRadian += diff;
    headingDegree = round((headingRadian * 4068) / 71.0);
    headingDegree = boundAngle(headingDegree);
    headingRadian = (headingDegree * 71 / 4068.0);
}

float LPS::getX() {
    return x;
}

float LPS::getY() {
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
