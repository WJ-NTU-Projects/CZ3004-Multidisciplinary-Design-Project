#include "Sensors.h"

Sensors::Sensors() {}

double Sensors::getDistance(int sensor) {
    if (sensor == 1) return getDistance(sensor1, A0m, A0c, A0r);
    if (sensor == 2) return getDistance(sensor2, A1m, A1c, A1r);
    if (sensor == 3) return getDistance(sensor3, A2m, A2c, A2r);
    if (sensor == 4) return getDistance(sensor4, A3m, A3c, A3r);
    if (sensor == 5) return getDistance(sensor5, A4m, A4c, A4r);
    if (sensor == 6) return getDistance(sensor6, A5m, A5c, A5r);
}

int Sensors::getPrintDistance(int sensor) {
    int distance = getDistanceR(sensor);
    distance = ceil(distance * 0.1);
    if (distance < 0) distance = 9;
    else distance = min(distance, 9);
    return distance;
}

int Sensors::getDistanceR(int sensor) {
    return round(getDistance(sensor));
}

double Sensors::getDistance(char sensor, double m, double c, double r) {
    int raw = analogRead(sensor);
    int voltsFromRaw = map(raw, 0, 1023, 0, 5000);
    double volts = voltsFromRaw * 0.001;
    return (1 / ((volts * m) + c)) - r;
}

double Sensors::getDistanceAverageFront() {
    double distance1 = getDistance(1);
    double distance2 = getDistance(2);  
    double distance3 = getDistance(3);  
    return (distance1 + distance2 + distance3) * 0.333333;
}

double Sensors::getErrorLeft() {
    double distance1 = getDistance(4);
    double distance2 = getDistance(5);    
    return (distance1 - distance2);
}

double Sensors::getErrorFront() {
    double distance1 = getDistance(1);
    double distance2 = getDistance(3);    
    return (distance1 - distance2);
}

boolean Sensors::mayAlignLeft() {
    double distance1 = getDistance(4);
    double distance2 = getDistance(5);    
    if (distance1 >= 2 && distance1 <= 10 && distance2 >= 2 && distance2 <= 10) return true;
    return false;
}

boolean Sensors::mayAlignFront() {
    double distance1 = getDistance(1);
    double distance2 = getDistance(3);    
    if (distance1 > 0 && distance1 <= 10 && distance2 > 0 && distance2 <= 10) return true;
    return false;
}

boolean Sensors::isObstructedFront() {
    double distance1 = getDistance(1);
    double distance2 = getDistance(2);
    double distance3 = getDistance(3);
    if (distance2 > 0 && distance2 <= 5.5) return true;
    if (distance1 > 0 && distance1 <= 5.5) return true;
    if (distance3 > 0 && distance3 <= 5.5) return true;
    return false;
}

boolean Sensors::isMovableLeft() {
    int distance1 = getDistanceR(4);
    int distance2 = getDistanceR(5);
    if (distance1 > 0 && distance1 <= 10) return false;
    if (distance2 > 0 && distance2 <= 10) return false;
    return true;
}

boolean Sensors::isNearFront() {
    double distance1 = getDistance(1);
    double distance2 = getDistance(2);
    double distance3 = getDistance(3);
    if (distance2 > 0 && distance2 <= 12) return true;
    if (distance1 > 0 && distance1 <= 12) return true;
    if (distance3 > 0 && distance3 <= 12) return true;
    return false;
}
