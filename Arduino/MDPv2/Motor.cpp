#include <digitalWriteFast.h>
#include "Motor.h"

Motor::Motor() {
    pinModeFast(INA1, OUTPUT);
    pinModeFast(INB1, OUTPUT);
    pinModeFast(INA2, OUTPUT);
    pinModeFast(INB2, OUTPUT);
    pinMode(PWM1, OUTPUT);
    pinMode(PWM2, OUTPUT);
}

void Motor::moveForward(double speedR, double speedL) {
    if (speedR <= 0 || speedL <= 0) return;
    if (speedR > 400) speedR = 400;
    if (speedL > 400) speedL = 400;
    
    analogWrite(PWM2, map(speedL, 0, 400, 0, 255));
    analogWrite(PWM1, map(speedR, 0, 400, 0, 255));
    digitalWriteFast2(INA2, HIGH);
    digitalWriteFast2(INB2, LOW);
    digitalWriteFast2(INA1, HIGH);
    digitalWriteFast2(INB1, LOW);
}

void Motor::moveReverse(double speedR, double speedL) {
    if (speedR <= 0 || speedL <= 0) return;
    if (speedR > 400) speedR = 400;
    if (speedL > 400) speedL = 400;
    
    analogWrite(PWM2, map(speedL, 0, 400, 0, 255));
    analogWrite(PWM1, map(speedR, 0, 400, 0, 255));
    digitalWriteFast2(INA2, LOW);
    digitalWriteFast2(INB2, HIGH);
    digitalWriteFast2(INA1, LOW);
    digitalWriteFast2(INB1, HIGH);
}

void Motor::turnRight(double speedR, double speedL) {
    if (speedR <= 0 || speedL <= 0) return;
    if (speedR > 400) speedR = 400;
    if (speedL > 400) speedL = 400;
    
    analogWrite(PWM2, map(speedL, 0, 400, 0, 255));
    analogWrite(PWM1, map(speedR, 0, 400, 0, 255));
    digitalWriteFast2(INA2, HIGH);
    digitalWriteFast2(INB2, LOW);
    digitalWriteFast2(INA1, LOW);
    digitalWriteFast2(INB1, HIGH);
}

void Motor::turnLeft(double speedR, double speedL) {
    if (speedR <= 0 || speedL <= 0) return;
    if (speedR > 400) speedR = 400;
    if (speedL > 400) speedL = 400;
    
    analogWrite(PWM2, map(speedL, 0, 400, 0, 255));
    analogWrite(PWM1, map(speedR, 0, 400, 0, 255));
    digitalWriteFast2(INA2, LOW);
    digitalWriteFast2(INB2, HIGH);
    digitalWriteFast2(INA1, HIGH);
    digitalWriteFast2(INB1, LOW);
}

void Motor::brake() {
    digitalWriteFast2(INA1, LOW);
    digitalWriteFast2(INB1, LOW);
    digitalWriteFast2(INA2, LOW);
    digitalWriteFast2(INB2, LOW);
    analogWrite(PWM1, 255);
    analogWrite(PWM2, 255);
}
