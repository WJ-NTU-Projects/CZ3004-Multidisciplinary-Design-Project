#pragma once
#include <Arduino.h>

#define sensor1 A0 // PS1 S
#define sensor2 A1 // PS2 L
#define sensor3 A2 // PS3 S
#define sensor4 A3 // PS4 S       // >7
#define sensor5 A4 // PS5 S       // >7
#define sensor6 A5 // PS6 L       

#define A0m 0.0359
#define A0c -0.0012
#define A1m 0.0113
#define A1c 0.003
#define A2m 0.0339
#define A2c 0.0003
#define A3m 0.0399
#define A3c -0.0021
#define A4m 0.0323
#define A4c 0.0031
#define A5m 0.0095
#define A5c 0.0034

#define A0r 7.39
#define A1r 13.44
#define A2r 7.52
#define A3r 7.28
#define A4r 7.64
#define A5r 17.72
#define FINE 1
#define COARSE 0

class Sensor {
    public:
        Sensor();
        double getSensorDistance(char, double, double, double);
        double getSensorAverageLeft();
        double getSensorErrorLeft();
        double getSensorErrorFront();
        boolean mayAlignFront();
        boolean mayAlignLeft();
        boolean hasObstacleFront(double);
        boolean hasObstacleLeft(double);
};
