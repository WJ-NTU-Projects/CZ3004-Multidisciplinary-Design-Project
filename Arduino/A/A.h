#pragma once
#include <EnableInterrupt.h>
#include "Motor.h"
#include "LPS.h"
#include "PID.h"
#include "Sensor.h"
#include "Config.h"

#define ENCODER_LEFT 11
#define ENCODER_RIGHT 3

// CONTROLS
boolean leftEmpty = false;
boolean moving = false;
boolean aligning = false;
boolean aligningFront = false;
boolean aligningLeft = false;
boolean sensor2Close = false;
int alignCounter = 0;
volatile boolean movingLeft = false;
volatile boolean movingRight = false;
unsigned long loopTime = 0;
unsigned long alignStartTime = 0;

// MOTOR
Motor motor;
volatile double ticksLeft = 0;
volatile double ticksRight = 0;
double ticksTarget = 0;
double speedLeft = 0;
double speedRight = 0;
int speedDefault = EXPLORE_SPEED_LEFT;
int currentDirection = FORWARD;
unsigned long lastMoveTime = 0;

// GP
int globalX = START_X;
int globalY = START_Y;
int globalHeading = START_HEADING;

// LPS
LPS lps(&ticksLeft, &ticksRight, TICKS_PER_MM);
double localX = 0;
double localY = 0;
int heading = 0;
int localRef = 0;

// Sensor
Sensor sensor;
double sensorError = 0;

// PID
double kp = 50;
double ki = 10;
double kd = 200;
double setpoint = 0;

PID sensorPID(&sensorError, &setpoint, kp, ki, kd);
PID pid(&localY, &setpoint, kp, ki, kd);
double speedOffset = 0;
