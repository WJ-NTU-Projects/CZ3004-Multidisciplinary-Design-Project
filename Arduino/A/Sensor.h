#pragma once
#include <Arduino.h>

#define sensor1 A0
#define sensor2 A1
#define sensor3 A2
#define sensor4 A4
#define sensor5 A3
#define sensor6 A5
#define S1m 0.0529
#define S1c -0.0108
#define S2m 0.0204
#define S2c -0.004
#define S3m 0.0529
#define S3c -0.0108
#define S4m 0.0454
#define S4c -0.004
#define S5m 0.0454
#define S5c -0.0046
#define S6m 0.0178
#define S6c -0.0012
#define FINE 1
#define COARSE 0

class Sensor {
    public:
        Sensor();
        double getSensorDistance1(int);
        double getSensorDistance2(int);
        double getSensorDistance3(int);
        double getSensorDistance4(int);
        double getSensorDistance5(int);
        double getSensorDistance6(int);
        boolean mayAlignFront();
        boolean mayAlignLeft();
        boolean hasObstacleFront(double);
        boolean hasObstacleLeft(double);
};
