#include "Motor.h"

Motor::Motor() {
    pinModeFast(IN_A_LEFT, OUTPUT);
    pinModeFast(IN_B_LEFT, OUTPUT);
    pinModeFast(IN_A_RIGHT, OUTPUT);
    pinModeFast(IN_B_RIGHT, OUTPUT);
    pinMode(PWM_LEFT, OUTPUT);
    pinMode(PWM_RIGHT, OUTPUT);
}

void Motor::forward(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    digitalWriteFast(IN_A_LEFT, HIGH);
    digitalWriteFast(IN_B_LEFT, LOW);
    digitalWriteFast(IN_A_RIGHT, HIGH);
    digitalWriteFast(IN_B_RIGHT, LOW);
}

void Motor::reverse(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    digitalWriteFast(IN_A_LEFT, LOW);
    digitalWriteFast(IN_B_LEFT, HIGH);
    digitalWriteFast(IN_A_RIGHT, LOW);
    digitalWriteFast(IN_B_RIGHT, HIGH);
}

void Motor::turnRight(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    digitalWriteFast(IN_A_LEFT, HIGH);
    digitalWriteFast(IN_B_LEFT, LOW);
    digitalWriteFast(IN_A_RIGHT, LOW);
    digitalWriteFast(IN_B_RIGHT, HIGH);
}

void Motor::turnLeft(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    digitalWriteFast(IN_A_LEFT, LOW);
    digitalWriteFast(IN_B_LEFT, HIGH);
    digitalWriteFast(IN_A_RIGHT, HIGH);
    digitalWriteFast(IN_B_RIGHT, LOW);
}

void Motor::setSpeed(int speedLeft, int speedRight) {
    if (speedLeft < 0 || speedRight < 0) return;
    if (speedLeft > 400) speedLeft = 400;
    if (speedRight > 400) speedRight = 400;

    analogWrite(PWM_LEFT, map(speedLeft, 0, 400, 0, 255));
    analogWrite(PWM_RIGHT, map(speedRight, 0, 400, 0, 255));
}

void Motor::brake() {
    digitalWriteFast(IN_A_LEFT, LOW);
    digitalWriteFast(IN_B_LEFT, LOW);
    digitalWriteFast(IN_A_RIGHT, LOW);
    digitalWriteFast(IN_B_RIGHT, LOW);
    analogWrite(PWM_RIGHT, 255);
    analogWrite(PWM_LEFT, 255);
}

void Motor::brakeLeftOnly() {
    digitalWriteFast(IN_A_LEFT, LOW);
    digitalWriteFast(IN_B_LEFT, LOW);
    analogWrite(PWM_LEFT, 255);
}

void Motor::brakeRightOnly() {
    digitalWriteFast(IN_A_RIGHT, LOW);
    digitalWriteFast(IN_B_RIGHT, LOW);
    analogWrite(PWM_RIGHT, 255);
}
