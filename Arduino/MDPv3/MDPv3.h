#pragma once

#define ENCODER_LEFT 11
#define ENCODER_RIGHT 3
#define UNSIGNED_LONG_MAX 4294967295

enum Direction {
    forward,
    reverse,
    left,
    right
};

Direction dir = forward;
Direction dirBeforeAlign = dir;

unsigned long lastMoveTime = 0;
unsigned long lastComputeTime = 0;
unsigned long lastRiseL = 0;
unsigned long lastRiseR = 0;
int ticksL = 0;
int ticksR = 0;

double rpmL = 0;
double rpmR = 0;
double rpmTargetL = 0;
double rpmTargetR = 0;
double speedL = 0;
double speedR = 0;
double distanceTravelled = 0;
int distanceTarget = 0;

boolean moving = false;
boolean turning = false;
boolean aligning = false;
boolean obstacleAheadChecklist = false;

Robot robot;
PID pidLeft(&rpmL, &speedL, &rpmTargetL, 2, 40, 0, DIRECT);
PID pidRight(&rpmR, &speedR, &rpmTargetR, 1.25, 32, 0, DIRECT);
DeadReckoning dr(&ticksL, &ticksR, 562.25, 30, 179);
