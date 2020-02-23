#include "Sensor.h"

Sensor::Sensor() {
    
}

float Sensor::getSensorDistance1(int accuracy) {
    int raw = analogRead(sensor1);
    float finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        float distance = (1 / (volts * S1m + S1c)) - 0.42;
        finalDistance += (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

float Sensor::getSensorDistance2(int accuracy) {
    int raw = analogRead(sensor2);
    float finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        float distance = (1 / (volts * S2m + S2c)) - 1.52;
        finalDistance += (distance > 30)? distance + 1 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

float Sensor::getSensorDistance3(int accuracy) {
    int raw = analogRead(sensor3);
    float finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        float distance = (1 / (volts * S3m + S3c)) - 0.42;
        finalDistance += (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

float Sensor::getSensorDistance4(int accuracy) {
    int raw = analogRead(sensor4);
    float finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        float distance = (1 / (volts * S4m + S4c)) - 0.42;
        finalDistance += (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

float Sensor::getSensorDistance5(int accuracy) {
    int raw = analogRead(sensor5);
    float finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        float distance = (1 / (volts * S5m + S5c)) - 0.42;
        finalDistance += (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

float Sensor::getSensorDistance6(int accuracy) {
    int raw = analogRead(sensor6);
    float finalDistance = 0;

    for (int i = 0; i < (accuracy == FINE? 25 : 1); i++) {
        float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
        float distance = (1 / (volts * S6m + S6c)) - 1.52;
        finalDistance += (distance < 25)? distance - 1 : distance;
    }

    return (accuracy == FINE? finalDistance / 25 : finalDistance);
}

bool Sensor::mayAlignFront() {
    float distance1 = getSensorDistance1(FINE);
    float distance2 = getSensorDistance2(FINE);
    float distance3 = getSensorDistance3(FINE);
    if (distance1 < 0 || distance1 > 20) return false;
    if (distance2 < 0 || distance2 > 20) return false;
    if (distance3 < 0 || distance3 > 20) return false;
    return true;
}

bool Sensor::mayAlignLeft() {
    float distance4 = getSensorDistance4(FINE);
    float distance5 = getSensorDistance5(FINE);
    if (distance4 < 0 || distance4 > 20) return false;
    if (distance5 < 0 || distance5 > 20) return false;
    return true;
}

bool Sensor::hasObstacleFront(float distance) {
    float distance1 = getSensorDistance1(COARSE);
    float distance2 = getSensorDistance2(COARSE);
    float distance3 = getSensorDistance3(COARSE);
    if (distance1 > 0 && distance1 <= distance) return true;
    if (distance2 > 0 && distance2 <= distance) return true;
    if (distance3 > 0 && distance3 <= distance) return true;
    return false;
}

bool Sensor::hasObstacleLeft(float distance) {
    float distance4 = getSensorDistance4(COARSE);
    float distance5 = getSensorDistance5(COARSE);
    if (distance4 > 0 && distance4 <= distance) return true;
    if (distance5 > 0 && distance5 <= distance) return true;
    return false;
}
