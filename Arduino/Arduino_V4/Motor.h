#pragma once

#include <Arduino.h>
#include <digitalWriteFast.h>

const int A_LEFT = 7;
const int B_LEFT = 8;
const int A_RIGHT = 2;
const int B_RIGHT = 4;
const int PWM_LEFT = 10;
const int PWM_RIGHT = 9;
const int ENCODER_LEFT = 11;
const int ENCODER_RIGHT = 3;

const int FORWARD = 1;
const int REVERSE = 2;
const int LEFT = 3;
const int RIGHT = 4;

class Motor {
    public:
        Motor();
        void init();
        void setSpeeds(int, int, int);
        void brake();
        
    private:
        void brakeLeft();
        void brakeRight();
};
