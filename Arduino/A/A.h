#pragma once
#include <EnableInterrupt.h>
#include "Motor.h"
#include "LPS.h"
#include "PID.h"
#include "Sensors.h"

#define ENCODER_LEFT 11
#define ENCODER_RIGHT 3

#define EXPLORE_SPEED_LEFT 150
#define FAST_SPEED_LEFT 300

#define TICKS_PER_MM 2.98
#define TICKS_PER_ANGLE 4.54

// CONTROLS
boolean fast = false;
boolean moving = false;
volatile boolean movingLeft = false;
volatile boolean movingRight = false;

// MOTOR
Motor motor;
volatile double ticksLeft = 0;
volatile double ticksRight = 0;
double ticksTarget = 0;

// LPS
double localX = 0;
double localY = 0;
LPS lps(&ticksLeft, &ticksRight, TICKS_PER_MM);

// Sensor
Sensors sensors;

// PID
double setpoint = 0;
double speedOffset = 0;
PID pid(&localY, &setpoint, 50, 10, 200);
