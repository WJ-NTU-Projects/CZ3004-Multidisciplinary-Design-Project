#pragma once
#include <EnableInterrupt.h>
#include "Motor.h"
#include "LPS.h"
#include "PID.h"
#include "Sensor.h"
#include "Config.h"

#define ENCODER_LEFT 11
#define ENCODER_RIGHT 3
#define FORWARD 1
#define REVERSE 2
#define LEFT 3
#define RIGHT 4
#define SPEED_SLOW 100

// CONTROLS
boolean moving = false;
volatile boolean movingLeft = false;
volatile boolean movingRight = false;
unsigned long loopTime = 0;

// MOTOR
Motor motor;
volatile unsigned int ticksLeft = 0;
volatile unsigned int ticksRight = 0;
unsigned int ticksTarget = 0;
float speedLeft = 0;
float speedRight = 0;
int speedDefault = EXPLORE_SPEED;
int speedMax = EXPLORE_SPEED_MAX;
int currentDirection = FORWARD;
unsigned long lastMoveTime = 0;

// GP
int globalX = START_X;
int globalY = START_Y;
int globalHeading = START_HEADING;

// LPS
LPS lps(&ticksLeft, &ticksRight, TICKS_PER_MM);
float localX = 0;
float localY = 0;
float setpointY = 0;
int heading = 0;
int localRef = 0;

// PID
PID pid(&localY, &setpointY, 1.0f, 0.05f, 0.25f);
float speedOffset = 0;

// Sensor
Sensor sensor;
