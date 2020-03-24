#pragma once
#include <Arduino.h>
#include <MedianFilterLib.h>

#define sensor1 A0 // PS1 S
#define sensor2 A1 // PS2 L
#define sensor3 A2 // PS3 S
#define sensor4 A3 // PS4 S       // >7
#define sensor5 A4 // PS5 S       // >7
#define sensor6 A5 // PS6 L       

#define A0m 0.0427
#define A0c -0.006
#define A1m 0.0338
#define A1c 0.0004
#define A2m 0.0414
#define A2c -0.0058
#define A3m 0.0348
#define A3c 0.0005
#define A4m 0.0434
#define A4c -0.0043
#define A5m 0.0017
#define A5c 0.005
//#define A5m 0.0028
//#define A5c 0.0066

#define A0r 5.77
#define A1r 7.62
#define A2r 5.89
#define A3r 7.45
#define A4r 5.37
#define A5r 99.99

class Sensors {
    public:
        Sensors();
        double getDistanceFast(char, double, double, double);
        double getDistanceFast(int);
        double getDistance(char, double, double, double);
        double getDistance(int);
        int getPrintDistance(int);
        double getErrorLeft();
        double getErrorFront();
        double getErrorFront1();
        double getErrorFront3();
        boolean isObstructedFront();
        boolean isNearFront();
        double getDistanceAverageFront();
        double getDistanceAverageLeft();
};
