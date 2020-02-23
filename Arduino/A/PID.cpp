#include "PID.h"

PID::PID(float *in, float *sp, float p, float i, float d) {
    input = in;
    setpoint = sp;

    float sampleTimeSeconds = 5 * 0.001;
    kp = p;
    ki = i * sampleTimeSeconds;
    kd = d / sampleTimeSeconds;
    reset();
}

float PID::computeOffset() {
    float in = *input;
    float error = *setpoint - in;
    float inputDifference = in - previousInput;
    outputSum += ki * error;
    outputSum = constrain(outputSum, OUTPUT_MIN, OUTPUT_MAX);

    float out = kp * error;
    out += outputSum - (kd * inputDifference);
    out = constrain(out, OUTPUT_MIN, OUTPUT_MAX);
    previousInput = in;
    return out;
}

void PID::reset() {
    outputSum = 0;
    previousInput = 0;
}
