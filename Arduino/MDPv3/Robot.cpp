#include "Robot.h"

Robot::Robot() {
    pinMode(INA1, OUTPUT);
    pinMode(INB1, OUTPUT);
    pinMode(INA2, OUTPUT);
    pinMode(INB2, OUTPUT);
    pinMode(PWM1, OUTPUT);
    pinMode(PWM2, OUTPUT);
}

void Robot::moveForward(double speedR, double speedL) {
    if (speedR <= 0 || speedL <= 0) return;
    if (speedR > 400) speedR = 400;
    if (speedL > 400) speedL = 400;
    
    analogWrite(PWM2, map(speedL, 0, 400, 0, 255));
    analogWrite(PWM1, map(speedR, 0, 400, 0, 255));
    digitalWrite(INA2, HIGH);
    digitalWrite(INB2, LOW);
    digitalWrite(INA1, HIGH);
    digitalWrite(INB1, LOW);
}

void Robot::moveBackward(double speedR, double speedL) {
    if (speedR <= 0 || speedL <= 0) return;
    if (speedR > 400) speedR = 400;
    if (speedL > 400) speedL = 400;
    
    analogWrite(PWM2, map(speedL, 0, 400, 0, 255));
    analogWrite(PWM1, map(speedR, 0, 400, 0, 255));
    digitalWrite(INA2, LOW);
    digitalWrite(INB2, HIGH);
    digitalWrite(INA1, LOW);
    digitalWrite(INB1, HIGH);
}

void Robot::turnRight(double speedR, double speedL) {
    if (speedR <= 0 || speedL <= 0) return;
    if (speedR > 400) speedR = 400;
    if (speedL > 400) speedL = 400;
    
    analogWrite(PWM2, map(speedL, 0, 400, 0, 255));
    analogWrite(PWM1, map(speedR, 0, 400, 0, 255));
    digitalWrite(INA2, HIGH);
    digitalWrite(INB2, LOW);
    digitalWrite(INA1, LOW);
    digitalWrite(INB1, HIGH);
}

void Robot::turnLeft(double speedR, double speedL) {
    if (speedR <= 0 || speedL <= 0) return;
    if (speedR > 400) speedR = 400;
    if (speedL > 400) speedL = 400;
    
    analogWrite(PWM2, map(speedL, 0, 400, 0, 255));
    analogWrite(PWM1, map(speedR, 0, 400, 0, 255));
    digitalWrite(INA2, LOW);
    digitalWrite(INB2, HIGH);
    digitalWrite(INA1, HIGH);
    digitalWrite(INB1, LOW);
}

void Robot::brake() {
    digitalWrite(INA1, LOW);
    digitalWrite(INB1, LOW);
    digitalWrite(INA2, LOW);
    digitalWrite(INB2, LOW);
    analogWrite(PWM1, 255);
    analogWrite(PWM2, 255);
}

boolean Robot::hasObstacleFront() {
    double distance1 = getSensorDistance(1);
    double distance2 = getSensorDistance(2);
    double distance3 = getSensorDistance(3);
    
    if (distance1 > 0 && distance1 <= 10) {
        Serial.println("Sensor1: " + String(distance1));
        return true;
    }
    
    if (distance2 > 0 && distance2 <= 10) {
        Serial.println("Sensor2: " + String(distance2));
        return true;
    }

    if (distance3 > 0 && distance3 <= 10) {
        Serial.println("Sensor3: " + String(distance3));
        return true;
    }
    
    return false;
}

boolean Robot::hasObstacleLeft() {
    double distance4 = getSensorDistance(4);
    double distance5 = getSensorDistance(5);
    
    if (distance4 > 0 && distance4 <= 6) {
        Serial.println("Sensor4: " + String(distance4));
        return true;
    }
    
    if (distance5 > 0 && distance5 <= 6) {
        Serial.println("Sensor5: " + String(distance5));
        return true;
    }
    
    return false;
}

double Robot::getSensorDistance(int sensor) {
    int raw;
    
    switch (sensor) {
        case 1: 
            raw = analogRead(sensor1);
            break;
        case 2: 
            raw = analogRead(sensor2);
            break;
        case 3: 
            raw = analogRead(sensor3);
            break;
        case 4: 
            raw = analogRead(sensor4);
            break;
        case 5: 
            raw = analogRead(sensor5);
            break;
        case 6: 
            raw = analogRead(sensor6);
            break;
    }
    
    double volts = map(raw, 0, 1023, 0, 5000) * 0.001;
    double distance = 999.9;
    double m = 0;
    double c = 0;

    switch (sensor) {
        case 2: 
            distance = (1 / (volts * S2m + S2c)) - 1.52;
            delay(10);
            return (distance > 30)? distance + 1 : distance;

        case 6:
            distance = (1 / (volts * S6m + S6c)) - 1.52;
            delay(10);
            return (distance < 25)? distance - 1 : distance;

        case 1:
            m = S1m;
            c = S1c;
            break;
            
        case 3:
            m = S3m;
            c = S3c;
            break;
            
        case 4:
            m = S4m;
            c = S4c;
            break;
            
        case 5:
            m = S5m;
            c = S5c;
            break;
            
        default: return;
    }

    distance = (1 / (volts * m + c)) - 0.42;
    delay(10);
    return (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
}
