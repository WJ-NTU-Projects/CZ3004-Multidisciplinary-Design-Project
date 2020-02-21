#include "PID.h"

PID::PID(float *in, float *out, int *sp, float p, float i, float d) {
    input = in;
    output = out;
    setPoint = sp;
    kp = p;
    ki = i;
    kd = d;
    setOutputLimits(0, 350);
    setTunings(kp, ki, kd);
}

void PID::compute() {
    float in = *input;
    float error = *setPoint - in;
    float inputDifference = in - lastInput;
    outputSum += (ki * error);
    if (outputSum > outputMax) outputSum = outputMax;
    else if (outputSum < outputMin) outputSum = outputMin;

    float out = kp * error;
    out += outputSum - kd * inputDifference;
    if (out > outputMax) out = outputMax;
    else if (out < outputMin) out = outputMin;
    *output = out;
    lastInput = in;
}

void PID::setTunings(float p, float i, float d) {
    float sampleTimeSeconds = 5 * 0.001;
    kp = p;
    ki = i * sampleTimeSeconds;
    kd = d / sampleTimeSeconds;
}

void PID::setOutputLimits(float minimum, float maximum) {
    if (minimum >= maximum) return;
    outputMin = minimum;
    outputMax = maximum;

    if (*output > outputMax) *output = outputMax;
    else if (*output < outputMin) *output = outputMin;

    if (outputSum > outputMax) outputSum = outputMax;
    else if (outputSum < outputMin) outputSum = outputMin;
}

void PID::reset() {
    outputSum = *output;
    lastInput = *input;
    if (outputSum > outputMax) outputSum = outputMax;
    else if (outputSum < outputMin) outputSum = outputMin;
}
