#pragma once

#define M1E1Right 11
#define M2E2Left 3
#define sensor1 A0
#define sensor2 A1
#define sensor3 A2
#define sensor4 A3
#define sensor5 A4
#define sensor6 A5
#define A0m 0.0444
#define A0c -0.0081
#define A1m 0.0171
#define A1c -0.0028
#define A2m 0.0433
#define A2c -0.0028
#define A3m 0.0374
#define A3c -0.003
#define A4m 0.0382
#define A4c -0.0018
#define A5m 0.0185
#define A5c -0.0034

#define DISTANCE_PER_ROTATION 18.8495559
#define WAVES_PER_ROTATION 562.25
#define WAVES_PER_GRID 298.28289
#define WAVES_PER_ANGLE_COMPASS 4.68
#define WAVES_PER_ANGLE_ARENA 4.6469
#define WAVES_BRAKE 149

// CONFIGURATIONS
#define MAX_SPEED 350
#define PID_SAMPLE_TIME 5
#define ON_PAPER false
#define DIAGONAL_EVADE true
#define SENSOR_ROLLING_AVERAGE_COUNT 25

volatile unsigned int wavesLimit = 0;  
volatile unsigned int wavesL = 0;
volatile unsigned int wavesR = 0;
volatile unsigned long pulseTimeNowL = 0;
volatile unsigned long pulseTimeNowR = 0;
volatile unsigned long pulseTimeLastL = 0;
volatile unsigned long pulseTimeLastR = 0;
volatile unsigned long pulsePeriodL = 0;
volatile unsigned long pulsePeriodR = 0;

volatile boolean moveEnabled = false;
volatile boolean obstacleAhead = false;
boolean avoiding = false;

double kp = 1.8, ki = 35, kd = 0;
double currentSpeedL = 0;
double currentSpeedR = 0;
double outputSpeedL = 0;
double outputSpeedR = 0;
double rpmL = 0;
double rpmR = 0;
double rpmTarget = 0;
double totalDistance = 0;

int sensorCounter[6] = {0, 0, 0, 0, 0, 0};
int sensorValues[6][SENSOR_ROLLING_AVERAGE_COUNT];
int sensorRaw[6] = {0, 580, 0, 0, 0, 0};
int sensorDistance[6] = {999, 999, 999, 999, 999, 999};
