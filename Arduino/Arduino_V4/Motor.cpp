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

void Motor::setSpeeds(int direction, int speedLeft, int speedRight) {
    switch (direction) {
        case FORWARD:
            speedLeft = abs(speedLeft);
            speedRight = abs(speedRight);
            break;
        case REVERSE:
            speedLeft = -abs(speedLeft);
            speedRight = -abs(speedRight);
            break;
        case LEFT:
            speedLeft = -abs(speedLeft);
            speedRight = abs(speedRight);
            break;
        case RIGHT:
            speedLeft = abs(speedLeft);
            speedRight = -abs(speedRight);
            break;
    }
    
    speedLeft = constrain(speedLeft, -400, 400);
    speedRight = constrain(speedRight, -400, 400); 
    
    if (speedLeft == 0) {
        brakeLeft();
    } else {
        analogWrite(PWM_LEFT, map(abs(speedLeft), 0, 400, 0, 255));
        
        if (speedLeft > 0) {
            digitalWriteFast(A_LEFT, HIGH);
            digitalWriteFast(B_LEFT, LOW);
        } else {
            digitalWriteFast(A_LEFT, LOW);
            digitalWriteFast(B_LEFT, HIGH);
        }
    }

    if (speedRight == 0) {
        brakeRight();
    } else {
        analogWrite(PWM_RIGHT, map(abs(speedRight), 0, 400, 0, 255));

        if (speedRight > 0) {
            digitalWriteFast(A_RIGHT, HIGH);
            digitalWriteFast(B_RIGHT, LOW);
        } else {
            digitalWriteFast(A_RIGHT, LOW);
            digitalWriteFast(B_RIGHT, HIGH);
        }
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
