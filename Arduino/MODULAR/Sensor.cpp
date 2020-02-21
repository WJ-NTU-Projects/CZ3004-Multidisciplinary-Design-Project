#include "Sensor.h"

Sensor::Sensor() {
    
}

float Sensor::getSensorDistance1() {
    int raw = analogRead(sensor1);
    float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
    float distance = (1 / (volts * S1m + S1c)) - 0.42;
    return (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
}

float Sensor::getSensorDistance2() {
    int raw = analogRead(sensor2);
    float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
    float distance = (1 / (volts * S2m + S2c)) - 1.52;
    return (distance > 30)? distance + 1 : distance;
}

float Sensor::getSensorDistance3() {
    int raw = analogRead(sensor3);
    float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
    float distance = (1 / (volts * S3m + S3c)) - 0.42;
    return (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
}

float Sensor::getSensorDistance4() {
    int raw = analogRead(sensor4);
    float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
    float distance = (1 / (volts * S4m + S4c)) - 0.42;
    return (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
}

float Sensor::getSensorDistance5() {
    int raw = analogRead(sensor5);
    float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
    float distance = (1 / (volts * S5m + S5c)) - 0.42;
    return (distance < 12)? distance - 0.5 : (distance > 20)? distance + 1 : (distance > 17.5)? distance + 0.5 : distance;
}

float Sensor::getSensorDistance6() {
    int raw = analogRead(sensor6);
    float volts = map(raw, 0, 1023, 0, 5000) * 0.001;
    float distance = (1 / (volts * S6m + S6c)) - 1.52;
    return (distance < 25)? distance - 1 : distance;
}

bool Sensor::mayAlignFront() {
    float distance1 = getSensorDistance1();
    float distance2 = getSensorDistance2();
    float distance3 = getSensorDistance3();
    if (distance1 < 0 || distance1 > 20) return false;
    if (distance2 < 0 || distance2 > 20) return false;
    if (distance3 < 0 || distance3 > 20) return false;
    return true;
}

bool Sensor::hasObstacleFront(float distance) {
    float distance1 = getSensorDistance1();
    float distance2 = getSensorDistance2();
    float distance3 = getSensorDistance3();
    if (distance1 > 0 && distance1 <= distance) return true;
    if (distance2 > 0 && distance2 <= distance) return true;
    if (distance3 > 0 && distance3 <= distance) return true;
    return false;
}

bool Sensor::hasObstacleLeft(float distance) {
    float distance4 = getSensorDistance4();
    float distance5 = getSensorDistance5();
    if (distance4 > 0 && distance4 <= distance) return true;
    if (distance5 > 0 && distance5 <= distance) return true;
    return false;
}
