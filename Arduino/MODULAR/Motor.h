#pragma once
#include <Arduino.h>
#include <digitalWriteFast.h>

#define IN_A_LEFT 2
#define IN_B_LEFT 4
#define IN_A_RIGHT 7
#define IN_B_RIGHT 8
#define PWM_LEFT 10
#define PWM_RIGHT 9

class Motor {
    public:
        Motor();  
        void forward(int, int);
        void reverse(int, int);
        void turnLeft(int, int);
        void turnRight(int, int);
        void brake();
        void brakeLeftOnly();
        void brakeRightOnly();
        void setSpeed(int, int);
};
