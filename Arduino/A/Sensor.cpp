#include "Sensor.h"

Sensor::Sensor() {
    
}

double Sensor::getSensorDistance(char sensor, double m, double c, double r) {
    double totalDistance = 0;

    for (int i = 0; i < 10; i++) {
        int raw = analogRead(sensor);
        int voltsFromRaw = map(raw, 0, 1023, 0, 5000);
        double volts = voltsFromRaw * 0.001;
        double distance = (1 / ((volts * m) + c)) - r;
        totalDistance += distance;
    }

    totalDistance *= 0.1;
    return totalDistance;
}

double Sensor::getSensorErrorFront() {
    double distance1 = getSensorDistance(sensor1, A0m, A0c, A0r);
    double distance3 = getSensorDistance(sensor3, A2m, A2c, A2r);
    return (distance1 - distance3);
}

double Sensor::getSensorErrorLeft() {
    double distance4 = getSensorDistance(sensor4, A3m, A3c, A3r);
    double distance5 = getSensorDistance(sensor5, A4m, A4c, A4r);
    return (distance4 - distance5);
}

double Sensor::getSensorAverageLeft() {
    double distance4 = getSensorDistance(sensor4, A3m, A3c, A3r);
    double distance5 = getSensorDistance(sensor5, A4m, A4c, A4r);
    return (distance4 + distance5) * 0.5;
}

bool Sensor::mayAlignFront() {
    double distance1 = getSensorDistance(sensor1, A0m, A0c, A0r);
    double distance3 = getSensorDistance(sensor3, A2m, A2c, A2r);
    if (distance1 < 0 || distance1 > 30) return false;
    if (distance3 < 0 || distance3 > 30) return false;
    return true;
}

bool Sensor::mayAlignLeft() {
    double distance4 = getSensorDistance(sensor4, A3m, A3c, A3r);
    double distance5 = getSensorDistance(sensor5, A4m, A4c, A4r);
    if (distance4 < 0 || distance4 > 30) return false;
    if (distance5 < 0 || distance5 > 30) return false;
    return true;
}

bool Sensor::hasObstacleFront(double distance) {
    double distance1 = getSensorDistance(sensor1, A0m, A0c, A0r);
    double distance3 = getSensorDistance(sensor3, A2m, A2c, A2r);
    if (distance1 > 0 && distance1 <= distance) return true;
    if (distance3 > 0 && distance3 <= distance) return true;
    return false;
}

bool Sensor::hasObstacleLeft(double distance) {
    double distance4 = getSensorDistance(sensor4, A3m, A3c, A3r);
    double distance5 = getSensorDistance(sensor5, A4m, A4c, A4r);
    if (distance4 > 0 && distance4 <= distance) return true;
    if (distance5 > 0 && distance5 <= distance) return true;
    return false;
}
