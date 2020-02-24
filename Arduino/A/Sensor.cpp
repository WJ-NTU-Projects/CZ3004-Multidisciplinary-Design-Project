#include "Sensor.h"

Sensor::Sensor() {
    
}

double Sensor::getSensorDistance1(int accuracy) {
    int raw = analogRead(sensor1);
    double finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        double volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        double distance = (1 / (volts * S1m + S1c)) - 0.42;
        finalDistance += (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

double Sensor::getSensorDistance2(int accuracy) {
    int raw = analogRead(sensor2);
    double finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        double volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        double distance = (1 / (volts * S2m + S2c)) - 1.52;
        finalDistance += (distance > 30)? distance + 1 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

double Sensor::getSensorDistance3(int accuracy) {
    int raw = analogRead(sensor3);
    double finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        double volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        double distance = (1 / (volts * S3m + S3c)) - 0.42;
        finalDistance += (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

double Sensor::getSensorDistance4(int accuracy) {
    int raw = analogRead(sensor4);
    double finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        double volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        double distance = (1 / (volts * S4m + S4c)) - 0.42;
        finalDistance += (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

double Sensor::getSensorDistance5(int accuracy) {
    int raw = analogRead(sensor5);
    double finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        double volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        double distance = (1 / (volts * S5m + S5c)) - 0.42;
        finalDistance += (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

double Sensor::getSensorDistance6(int accuracy) {
    int raw = analogRead(sensor6);
    double finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        double volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        double distance = (1 / (volts * S6m + S6c)) - 1.52;
        finalDistance += (distance < 25)? distance - 1 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

bool Sensor::mayAlignFront() {
    double distance1 = getSensorDistance1(FINE);
    double distance2 = getSensorDistance2(FINE);
    double distance3 = getSensorDistance3(FINE);
    if (distance1 < 0 || distance1 > 20) return false;
    if (distance2 < 0 || distance2 > 20) return false;
    if (distance3 < 0 || distance3 > 20) return false;
    return true;
}

bool Sensor::mayAlignLeft() {
    double distance4 = getSensorDistance4(FINE);
    double distance5 = getSensorDistance5(FINE);
    if (distance4 < 0 || distance4 > 20) return false;
    if (distance5 < 0 || distance5 > 20) return false;
    return true;
}

bool Sensor::hasObstacleFront(double distance) {
    double distance1 = getSensorDistance1(COARSE);
    double distance2 = getSensorDistance2(COARSE);
    double distance3 = getSensorDistance3(COARSE);
    if (distance1 > 0 && distance1 <= distance) return true;
    if (distance2 > 0 && distance2 <= distance) return true;
    if (distance3 > 0 && distance3 <= distance) return true;
    return false;
}

bool Sensor::hasObstacleLeft(double distance) {
    double distance4 = getSensorDistance4(COARSE);
    double distance5 = getSensorDistance5(COARSE);
    if (distance4 > 0 && distance4 <= distance) return true;
    if (distance5 > 0 && distance5 <= distance) return true;
    return false;
}
