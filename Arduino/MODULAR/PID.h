#pragma once
#include <Arduino.h>

class PID {
    public:
        PID(float*, float*, int*, float, float, float);
        void compute();
        void setOutputLimits(float, float);
        void setSampleTime(int);
        void setTunings(float, float, float);
        void reset();

    private:
        float kp;
        float ki;
        float kd;
        float *input;
        float *output;
        int *setPoint;
        float outputSum;
        float lastInput;
        float outputMin;
        float outputMax;
};
