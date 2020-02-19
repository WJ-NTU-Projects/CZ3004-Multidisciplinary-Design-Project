#pragma once

#include <Arduino.h>

#define INA1 2
#define INB1 4
#define PWM1 9
#define INA2 7
#define INB2 8
#define PWM2 10

class Motor {
    public:
        Motor();
        void init();
        void moveForward(double speedR, double speedL);
        void moveReverse(double speedR, double speedL);
        void turnLeft(double speedR, double speedL);
        void turnRight(double speedR, double speedL);
        void brake();
};
