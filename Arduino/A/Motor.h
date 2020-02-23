#pragma once

#include <Arduino.h>
#include <digitalWriteFast.h>

#define A_LEFT 2
#define B_LEFT 4
#define A_RIGHT 7
#define B_RIGHT 8
#define PWM_LEFT 10
#define PWM_RIGHT 9

class Motor {
    public:
        Motor();
        void forward(int speedLeft, int speedRight);
        void reverse(int speedLeft, int speedRight);
        void turnLeft(int speedLeft, int speedRight);
        void turnRight(int speedLeft, int speedRight);
        void brakeLeft();
        void brakeRight();
        void setSpeed(int speedLeft, int speedRight);
        
    private:
};
