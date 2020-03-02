#include "Motor.h"

Motor::Motor() {
    pinModeFast(A_LEFT, OUTPUT);
    pinModeFast(B_LEFT, OUTPUT);
    pinModeFast(A_RIGHT, OUTPUT);
    pinModeFast(B_RIGHT, OUTPUT);
    pinMode(PWM_LEFT, OUTPUT);
    pinMode(PWM_RIGHT, OUTPUT);
}

void Motor::test() {
    forward(200, 0);
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
    if (speedLeft > 400) speedLeft = 400;
    if (speedRight > 400) speedRight = 400;
    
    if (speedLeft > 0) analogWrite(PWM_LEFT, map(speedLeft, 0, 400, 0, 255));
    else brakeLeft();
    
    if (speedRight > 0) analogWrite(PWM_RIGHT, map(speedRight, 0, 400, 0, 255));
    else brakeRight();
}

void Motor::forward(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    
    if (speedLeft > 0) {
        digitalWriteFast(A_LEFT, HIGH);
        digitalWriteFast(B_LEFT, LOW);
    }

    if (speedRight > 0) {
        digitalWriteFast(A_RIGHT, HIGH);
        digitalWriteFast(B_RIGHT, LOW);
    }
}

void Motor::reverse(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    
    if (speedLeft > 0) {
        digitalWriteFast(A_LEFT, LOW);
        digitalWriteFast(B_LEFT, HIGH);
    }

    if (speedRight > 0) {
        digitalWriteFast(A_RIGHT, LOW);
        digitalWriteFast(B_RIGHT, HIGH);
    }
}

void Motor::turnLeft(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    
    if (speedLeft > 0) {
        digitalWriteFast(A_LEFT, LOW);
        digitalWriteFast(B_LEFT, HIGH);
    }

    if (speedRight > 0) {
        digitalWriteFast(A_RIGHT, HIGH);
        digitalWriteFast(B_RIGHT, LOW);
    }
}

void Motor::turnRight(int speedLeft, int speedRight) {
    setSpeed(speedLeft, speedRight);
    
    if (speedLeft > 0) {
        digitalWriteFast(A_LEFT, HIGH);
        digitalWriteFast(B_LEFT, LOW);
    }

    if (speedRight > 0) {
        digitalWriteFast(A_RIGHT, LOW);
        digitalWriteFast(B_RIGHT, HIGH);
    }
}
