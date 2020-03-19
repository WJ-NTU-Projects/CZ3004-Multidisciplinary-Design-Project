#include "Motor.h"

Motor::Motor() {}

void Motor::init() {
    pinModeFast(A_LEFT, OUTPUT);
    pinModeFast(B_LEFT, OUTPUT);
    pinModeFast(A_RIGHT, OUTPUT);
    pinModeFast(B_RIGHT, OUTPUT);
    pinMode(PWM_LEFT, OUTPUT);
    pinMode(PWM_RIGHT, OUTPUT);
    pinMode(ENCODER_LEFT, INPUT);
    pinMode(ENCODER_RIGHT, INPUT);
    digitalWrite(ENCODER_LEFT, HIGH);       
    digitalWrite(ENCODER_RIGHT, HIGH);    
}

void Motor::move(int direction, int speedLeft, int speedRight) {
    switch (direction) {
        case FORWARD:
            forward(speedLeft, speedRight);
            break;
        case REVERSE:
            reverse(speedLeft, speedRight);
            break;
        case LEFT:
            turnLeft(speedLeft, speedRight);
            break;
        case RIGHT:
            turnRight(speedLeft, speedRight);
            break;
    }
}

void Motor::brake() {
    brakeRight();
    brakeLeft();
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
    delay(2);
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
