#include "Motor.h"

Motor::Motor() {
    pinModeFast(A_LEFT, OUTPUT);
    pinModeFast(B_LEFT, OUTPUT);
    pinModeFast(A_RIGHT, OUTPUT);
    pinModeFast(B_RIGHT, OUTPUT);
    pinMode(PWM_LEFT, OUTPUT);
    pinMode(PWM_RIGHT, OUTPUT);
}

void Motor::brakeLeft() {
    digitalWriteFast(A_LEFT, LOW);
    digitalWriteFast(B_LEFT, LOW);
    analogWrite(PWM_LEFT, 255);
}

void Motor::brakeRight() {
    digitalWriteFast(A_RIGHT, LOW);
    digitalWriteFast(B_RIGHT, LOW);
    analogWrite(PWM_RIGHT, 255);
}

void Motor::setSpeed(int speedLeft, int speedRight) {
    if (speedRight <= 0) {
        brakeRight();
        return;
    }

    if (speedLeft <= 0) {
        brakeLeft();
        return;
    }

    if (speedLeft > 400) speedLeft = 400;
    if (speedRight > 400) speedRight = 400;
    analogWrite(PWM_LEFT, map(speedLeft, 0, 400, 0, 255));
    analogWrite(PWM_RIGHT, map(speedRight, 0, 400, 0, 255));
}

void Motor::forward(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    digitalWriteFast(A_LEFT, HIGH);
    digitalWriteFast(B_LEFT, LOW);
    digitalWriteFast(A_RIGHT, HIGH);
    digitalWriteFast(B_RIGHT, LOW);
}

void Motor::reverse(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    digitalWriteFast(A_LEFT, LOW);
    digitalWriteFast(B_LEFT, HIGH);
    digitalWriteFast(A_RIGHT, LOW);
    digitalWriteFast(B_RIGHT, HIGH);
}

void Motor::turnLeft(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    digitalWriteFast(A_LEFT, LOW);
    digitalWriteFast(B_LEFT, HIGH);
    digitalWriteFast(A_RIGHT, HIGH);
    digitalWriteFast(B_RIGHT, LOW);
}

void Motor::turnRight(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    digitalWriteFast(A_LEFT, HIGH);
    digitalWriteFast(B_LEFT, LOW);
    digitalWriteFast(A_RIGHT, LOW);
    digitalWriteFast(B_RIGHT, HIGH);
}
