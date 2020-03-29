#pragma once
#include <Arduino.h>

class PID {
    public:
        PID(double*, double*, double, double, double);
        double computeOffset();
        void reset();
        
    private:
        double kp;
        double ki;
        double kd;
        double *input;
        double *setpoint;
        double previousError;
        double integral;
};
