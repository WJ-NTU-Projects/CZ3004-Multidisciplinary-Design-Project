#include "Position.h"

Position::Position(int *tl, int *tr, float tpmm) {
    ticksLeft = tl;
    ticksRight = tr;
    ticksPerMilli = tpmm * 1.0;
    reset();
}

void Position::reset() {
    x = 0;
    y = 0;
    heading = 0;
    headingDegree = 0;
    deltaLeft = 0;
    deltaRight = 0;
}

void Position::compute() {
    deltaLeft = *ticksLeft / ticksPerMilli;
    deltaRight = *ticksRight / ticksPerMilli;
    
    if (fabs(deltaLeft - deltaRight) < 1.0e-6) {
        x += deltaLeft * cos(heading);
        y += deltaRight * sin(heading);
        return;
    }

    float radius = WHEEL_WIDTH * (deltaLeft + deltaRight) / (2 * (deltaRight - deltaLeft));
    float diff = (deltaRight - deltaLeft) / WHEEL_WIDTH;
    x = x + (radius * sin(heading + diff) - radius * sin(heading));
    y = y - (radius * cos(heading + diff) + radius * cos(heading));
    heading = (heading + diff);
    headingDegree = round((heading * 4068) / 71.0) % 360;
    headingDegree = boundAngle(headingDegree);
    heading = (headingDegree * 71 / 4068.0);
}

float Position::getX() {
    return x;
}

float Position::getY() {
    return y;
}

int Position::getHeading() {
    return headingDegree;
}

int Position::boundAngle(int angle) {
    if (angle >= 360) angle -= 360;
    if (angle <= -360) angle += 360;
    return angle; 
}
