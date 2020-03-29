#include "PID.h"

PID::PID(double *in, double *sp, double p, double i, double d) {
    input = in;
    setpoint = sp;

    kp = p;
    ki = i * 0.1;
    kd = d * 10;
    reset();
}

double PID::computeOffset() {
    double in = *input;
    double error = *setpoint - in;
    integral += error;
    integral = constrain(integral, -255, 255);
    double derivative = error - previousError;
    double output = kp * error + ki * integral + kd * derivative;
    previousError = error;
    return output;
}

void PID::reset() {
    integral = 0;
    previousError = 0;
}
