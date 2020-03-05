#pragma once
#include <EnableInterrupt.h>
#include "Motor.h"
#include "LPS.h"
#include "PID.h"
#include "Sensors.h"

#define ENCODER_LEFT 11
#define ENCODER_RIGHT 3

#define EXPLORE_SPEED 260
#define FAST_SPEED 350

// 2.89
// 4.591
#define TICKS_PER_MM 2.96
#define TICKS_MULTIPLIER 0.41
//#define TICKS_PER_MM 2.92
#define TICKS_PER_ANGLE 4.58

volatile boolean continuousMovement = false;
volatile boolean movingLeft = false;
volatile boolean movingRight = false;
double frontDistanceReference = 0;
double ticksReference = 0;
double leftErrorReference = 0;
double ticksTarget = 0;
volatile double ticksLeft = 0;
volatile double ticksRight = 0;
double speedMax = EXPLORE_SPEED;
double speedLeft = 0;
double speedRight = 0;
double error = 0;
double setpoint = 0;
double currentSpeedLeft = 0;
double currentSpeedRight = 0;

Motor motor;
Sensors sensors;
LPS lps(&ticksLeft, &ticksRight, TICKS_MULTIPLIER);
PID leftAlignPID(&error, &setpoint, 50, 10, 200);
PID pid(&error, &setpoint, 50, 10, 200);
