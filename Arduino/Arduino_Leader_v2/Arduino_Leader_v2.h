#pragma once
#include <EnableInterrupt.h>
#include "Motor.h"
#include "LPS.h"
#include "PID.h"
#include "Sensors.h"

#define ENCODER_LEFT 11
#define ENCODER_RIGHT 3

#define TURN_SPEED_LEFT 180
#define EXPLORE_SPEED_LEFT 250
#define FAST_SPEED_LEFT 300

#define TICKS_PER_MM 2.92
#define TICKS_MULTIPLIER 0.41
//#define TICKS_PER_MM 2.92
#define TICKS_PER_ANGLE 4.525

double leftErrorReference = 0;
double ticksTarget = 0;
volatile double ticksLeft = 0;
volatile double ticksRight = 0;
double speedLeftRef = EXPLORE_SPEED_LEFT;
double speedRightRef = EXPLORE_SPEED_LEFT - 30;
double speedLeft = 0;
double speedRight = 0;
double error = 0;
double setpoint = 0;

Motor motor;
Sensors sensors;
LPS lps(&ticksLeft, &ticksRight, TICKS_MULTIPLIER);
PID leftAlignPID(&error, &setpoint, 50, 10, 200);
PID pid(&error, &setpoint, 50, 10, 200);
