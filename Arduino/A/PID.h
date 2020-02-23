#pragma once
#include <Arduino.h>

#define OUTPUT_MAX 350
#define OUTPUT_MIN 0

class PID {
    public:
        PID(float*, float*, float, float, float);
        float computeOffset();
        void reset();
        
    private:
        float kp;
        float ki;
        float kd;
        float *input;
        float *setpoint;
        float previousInput;
        float outputSum;
};
