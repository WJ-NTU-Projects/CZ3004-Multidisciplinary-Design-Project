#pragma once
#include <EnableInterrupt.h>
#include "Motor.h"
#include "LPS.h"
#include "PID.h"
#include "Sensors.h"
#include "Config.h"

#define ENCODER_LEFT 11
#define ENCODER_RIGHT 3

// CONTROLS
boolean moving = false;
boolean aligned = false;
boolean sensor2Close = false;
volatile boolean movingLeft = false;
volatile boolean movingRight = false;

// MOTOR
Motor motor;
volatile double ticksLeft = 0;
volatile double ticksRight = 0;
double ticksTarget = 0;
double speedLeft = 0;
double speedRight = 0;
int speedDefault = EXPLORE_SPEED_LEFT;
unsigned long lastMoveTime = 0;

// LPS
LPS lps(&ticksLeft, &ticksRight, TICKS_PER_MM);
double localX = 0;
double localY = 0;

// Sensor
Sensors sensors;

// PID
double kp = 50;
double ki = 10;
double kd = 200;
double setpoint = 0;

PID pid(&localY, &setpoint, kp, ki, kd);
double speedOffset = 0;
