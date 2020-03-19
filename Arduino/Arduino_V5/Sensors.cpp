#include "Sensors.h"

Sensors::Sensors() {}

void swap(double *xp, double *yp)  {  
    double temp = *xp;  
    *xp = *yp;  
    *yp = temp;  
}  
  
// A function to implement bubble sort  
void bubbleSort(double arr[], int n)  {  
    int i, j;  
    for (i = 0; i < n-1; i++)      
        // Last i elements are already in place  
        for (j = 0; j < n-i-1; j++)  
            if (arr[j] > arr[j+1])  
                swap(&arr[j], &arr[j+1]);  
} 

int Sensors::getPrintDistance(int sensor) {
    int distance = getDistance(sensor);
    distance = ceil(distance * 0.1 + 0.3);
    if (distance < 0) distance = 9;
    else distance = min(distance, 9);
    return distance;
} 

double Sensors::getDistanceFast(int sensor) {
    if (sensor == 1) return getDistanceFast(sensor1, A0m, A0c, A0r);
    if (sensor == 2) return getDistanceFast(sensor2, A1m, A1c, A1r);
    if (sensor == 3) return getDistanceFast(sensor3, A2m, A2c, A2r);
    if (sensor == 4) return getDistanceFast(sensor4, A3m, A3c, A3r);
    if (sensor == 5) return getDistanceFast(sensor5, A4m, A4c, A4r);
    if (sensor == 6) return getDistanceFast(sensor6, A5m, A5c, A5r);
}

double Sensors::getDistance(int sensor) {
    if (sensor == 1) return getDistance(sensor1, A0m, A0c, A0r);
    if (sensor == 2) return getDistance(sensor2, A1m, A1c, A1r);
    if (sensor == 3) return getDistance(sensor3, A2m, A2c, A2r);
    if (sensor == 4) return getDistance(sensor4, A3m, A3c, A3r);
    if (sensor == 5) return getDistance(sensor5, A4m, A4c, A4r);
    if (sensor == 6) return getDistance(sensor6, A5m, A5c, A5r);
}

double Sensors::getDistanceFast(char sensor, double m, double c, double r) {
    int readingsCount = 3;
    int medianPosition = 1;
    double values[readingsCount];

    for (int i = 0; i < readingsCount; i++) {
        int raw = analogRead(sensor);
        int voltsFromRaw = map(raw, 0, 1023, 0, 5000);
        double volts = voltsFromRaw * 0.001;
        double distance = (1 / ((volts * m) + c)) - r;
        if (sensor == sensor6) distance -= 8;
        values[i] = distance;
    }

    bubbleSort(values, readingsCount);
    double median = values[medianPosition];
    if (median > 70) median = 70;
    if (median < 0) median = 70;
    return median;    
}

double Sensors::getDistance(char sensor, double m, double c, double r) {
    int readingsCount = 51;
    int medianPosition = 25;
    double values[readingsCount];

    for (int i = 0; i < readingsCount; i++) {
        int raw = analogRead(sensor);
        int voltsFromRaw = map(raw, 0, 1023, 0, 5000);
        double volts = voltsFromRaw * 0.001;
        double distance = (1 / ((volts * m) + c)) - r;
        if (sensor == sensor6) distance -= 8;
        values[i] = distance;
    }

    bubbleSort(values, readingsCount);
    double median = values[medianPosition];
    if (median > 70) median = 70;
    if (median < 0) median = 70;
    return median;    
}

double Sensors::getDistanceAverageFront() {
    double distance1 = getDistance(1);
    //double distance2 = getDistance(2);  
    double distance3 = getDistance(3);  
    return (distance1 + distance3) * 0.5;
}

double Sensors::getDistanceAverageLeft() {
    double distance1 = getDistance(4);
    double distance2 = getDistance(5);  
    return (distance1 + distance2) * 0.5;
}

double Sensors::getErrorLeft() {
    double distance1 = getDistance(4);
    double distance2 = getDistance(5);    
    return (distance1 - distance2);
}

double Sensors::getErrorFront1() {
    double distance1 = getDistance(1);
    double distance2 = getDistance(2);    
    return (distance1 - distance2);
}

double Sensors::getErrorFront3() {
    double distance1 = getDistance(2);
    double distance2 = getDistance(3);    
    return (distance1 - distance2);
}

double Sensors::getErrorFront() {
    double distance1 = getDistance(1);
    double distance2 = getDistance(3);    
    return (distance1 - distance2);
}

boolean Sensors::isObstructedFront() {
    double distance1 = getDistanceFast(1);
    double distance2 = getDistanceFast(2);
    double distance3 = getDistanceFast(3);
    if (distance2 > 0 && distance2 <= 5) return true;
    if (distance1 > 0 && distance1 <= 5) return true;
    if (distance3 > 0 && distance3 <= 5) return true;
    return false;
}

boolean Sensors::isNearFront() {
    double distance1 = getDistanceFast(1);
    double distance2 = getDistanceFast(2);
    double distance3 = getDistanceFast(3);
    if (distance2 > 0 && distance2 <= 8) return true;
    if (distance1 > 0 && distance1 <= 8) return true;
    if (distance3 > 0 && distance3 <= 8) return true;
    return false;
}
