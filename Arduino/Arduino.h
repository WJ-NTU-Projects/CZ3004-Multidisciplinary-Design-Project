#pragma once
#include <EnableInterrupt.h>
#include "Motor.h"
#include "LPS.h"
#include "PID.h"
#include "Sensors.h"

#define ENCODER_LEFT 11
#define ENCODER_RIGHT 3

#define EXPLORE_SPEED_LEFT 160
#define FAST_SPEED_LEFT 300

#define TICKS_PER_MM 2.98
#define TICKS_PER_ANGLE 4.54

volatile boolean movingLeft = false;
volatile boolean movingRight = false;
volatile double ticksLeft = 0;
volatile double ticksRight = 0;

boolean automate = false;
boolean moving = false;
boolean fast = false;
double ticksTarget = 0;
double localX = 0;
double localY = 0;
double setpoint = 0;
double speedOffset = 0;

Motor motor;
Sensors sensors;
LPS lps(&ticksLeft, &ticksRight, TICKS_PER_MM);
PID pid(&localY, &setpoint, 50, 10, 200);
