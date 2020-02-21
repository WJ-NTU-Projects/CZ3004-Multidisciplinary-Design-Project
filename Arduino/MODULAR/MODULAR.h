#pragma once
#include <EnableInterrupt.h>
#include "PID.h"
#include "Motor.h"
#include "Sensor.h"
#include "Position.h"

#define ENCODER_LEFT 11
#define ENCODER_RIGHT 3
#define FORWARD 1
#define REVERSE 2
#define LEFT 3
#define RIGHT 4
#define MAX_SPEED 250
#define TICKS_PER_MM 29828
#define TICKS_PER_ANGLE 466

int currentDirection = FORWARD;
int ticksLeft = 0;
int ticksRight = 0;
int ticksTarget = 0;
int distanceTarget = 0;

float speedLeft = 0;
float speedRight = 0;
float speedOffset = 0;
float positionX = 0;
float positionY = 0;
float heading = 0;
int positionXError = 0;
int positionYSetPoint = 0;

volatile boolean newTick = false;
boolean moving = false;
boolean movingLeft = false;
boolean movingRight = false;
boolean turning = false;
boolean aligning = false;
boolean leftReached = false;
boolean rightReached = false;

unsigned long lastMoveTime = 0;

Motor motor;
Sensor sensor;
Position position(&ticksLeft, &ticksRight, TICKS_PER_MM);
PID pid(&positionY, &speedOffset, &positionYSetPoint, 1.0f, 0.05f, 0.25f);
