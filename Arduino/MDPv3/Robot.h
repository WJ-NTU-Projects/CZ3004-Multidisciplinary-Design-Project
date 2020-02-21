#pragma once

#include <Arduino.h>

#define INA1 2
#define INB1 4
#define PWM1 9
#define INA2 7
#define INB2 8
#define PWM2 10

#define sensor1 A0
#define sensor2 A1
#define sensor3 A2
#define sensor4 A3
#define sensor5 A4
#define sensor6 A5
#define S1m 0.0529
#define S1c -0.0108
#define S2m 0.0204
#define S2c -0.004
#define S3m 0.0529
#define S3c -0.0108
#define S4m 0.0454
#define S4c -0.004
#define S5m 0.0454
#define S5c -0.0046
#define S6m 0.0178
#define S6c -0.0012

class Robot {
    public: 
        Robot();
        void moveForward(double speedR, double speedL);
        void moveBackward(double speedR, double speedL);
        void turnRight(double speedR, double speedL);
        void turnLeft(double speedR, double speedL);
        void brake();
        boolean hasObstacleFront();
        boolean hasObstacleLeft();
        double getSensorDistance(int);
};
