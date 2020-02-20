#include <EnableInterrupt.h>
#include <PID_v1.h>
#include "MDPv2.h"
#include "Motor.h"

Motor motor;
PID pidLeft(&rpmL, &outputSpeedL, &rpmTarget, 2, 40, kd, DIRECT);
PID pidRight(&rpmR, &outputSpeedR, &rpmTarget, 2, 35, kd, DIRECT);
int loopCounter = 1;

void setup() {
    pinMode(M1E1Right, INPUT);
    pinMode(M2E2Left, INPUT);
    digitalWrite(M1E1Right, HIGH);       
    digitalWrite(M2E2Left, HIGH);       
    enableInterrupt(M1E1Right, E1, RISING);
    enableInterrupt(M2E2Left, E2, RISING);

    Serial.begin(115200);
    pidLeft.SetOutputLimits(-MAX_SPEED, MAX_SPEED);  
    pidLeft.SetSampleTime(PID_SAMPLE_TIME);
    pidRight.SetOutputLimits(-MAX_SPEED, MAX_SPEED);   
    pidRight.SetSampleTime(PID_SAMPLE_TIME);
    enablePID(false);

    delay(3000);
    moveRobot(forward, 15);
    return;
    delay(1000);
    turnRobot(right, 90);
    delay(500);
    turnRobot(right, 90);
    delay(500);
    turnRobot(right, 90);
    delay(500);
    turnRobot(right, 90);
    delay(500);
    turnRobot(left, 90);
    delay(500);
    turnRobot(left, 90);
    delay(500);
    turnRobot(left, 90);
    delay(500);
    turnRobot(left, 90);
    delay(500);
    turnRobot(right, 850);
    delay(500);
    turnRobot(right, 230);
    delay(500);
    turnRobot(left, 850);
    delay(500);
    turnRobot(left, 230);
}

void loop() {
//    Serial.println(getIRDistance(sensor1, A0m, A0c));
//    Serial.println(getIRDistance(sensor2, A1m, A1c));
//    Serial.println(getIRDistance(sensor3, A2m, A2c));
//    Serial.println();
//    delay(1000);
//    moveRobot(forward, loopCounter);
//    delay(3000);
//    moveRobot(reverse, loopCounter);
//    delay(3000);
//    loopCounter++;
//    if (loopCounter > 4) loopCounter = 1;
//    delay(500);
}

void enablePID(boolean a) {
    pidLeft.SetMode(a? AUTOMATIC : MANUAL);
    pidRight.SetMode(a? AUTOMATIC : MANUAL);
}

void reset() {
    wavesL = wavesR = rpmL = rpmR = currentSpeedL = currentSpeedR = outputSpeedL = outputSpeedR = pulsePeriodL = pulsePeriodR = 0;
    moveEnabled = true;
    pulseTimeLastL = micros();
    pulseTimeLastR = micros();
}

void moveRobot(Direction direction, int distance) {
    if (obstacleAhead) return;
    if (direction == left || direction == right) return;
    reset();
    rpmTarget = 30;
    enablePID(true);
    wavesLimit = round(WAVES_PER_GRID * distance);

    while (moveEnabled) {
        //if (!obstacleAhead) totalDistance = wavesL / WAVES_PER_ROTATION * DISTANCE_PER_ROTATION;
        //if (direction == forward) checkForObstacleAhead();
        pidLeft.Compute();
        pidRight.Compute();
        currentSpeedL = max(outputSpeedL, 0); //150cm 1.061 
        currentSpeedR = max(outputSpeedR, 0); 
        if (direction == forward) motor.moveForward(currentSpeedR, currentSpeedL);
        else if (direction == reverse) motor.moveReverse(currentSpeedR, currentSpeedL);
        rpmL = (pulsePeriodL == 0)? 0 : max(round(60000000.0 / (pulsePeriodL * WAVES_PER_ROTATION)), 0);
        rpmR = (pulsePeriodR == 0)? 0 : max(round(60000000.0 / (pulsePeriodR * WAVES_PER_ROTATION)), 0); 
        
        if (wavesL >= wavesLimit - (WAVES_PER_GRID * (MAX_RPM * 0.01)) || wavesR >= wavesLimit - (WAVES_PER_GRID * (MAX_RPM * 0.01))) {
            if (rpmTarget > 20 && round(rpmL) <= rpmTarget && round(rpmR) <= rpmTarget) {
                rpmTarget -= 10;
            }
        } else {
            if (rpmTarget < MAX_RPM && round(rpmL) >= rpmTarget && round(rpmR) >= rpmTarget) {
                rpmTarget += 10;
            }
        }
        
        Serial.println("Speed: " + String(currentSpeedL) + " / " + String(currentSpeedR) + ", RPM: " + String(round(rpmL)) + " / " + String(round(rpmR)) + ", RPM Target: " + String(rpmTarget));
    }
    
    motor.brake();
    enablePID(false);
    reset();
}

void turnRobot(Direction direction, int angle) {
    if (direction == forward || direction == reverse) return;
    reset();
    rpmTarget = 40;
    enablePID(true);
    wavesLimit = round((direction == right? WAVES_PER_ANGLE_RIGHT : WAVES_PER_ANGLE_LEFT) * angle);

    while (moveEnabled) {
        pidLeft.Compute();
        pidRight.Compute();
        currentSpeedL = max(outputSpeedL, 0); //150cm 1.061 
        currentSpeedR = max(outputSpeedR, 0); 
        if (direction == right) motor.turnRight(currentSpeedR, currentSpeedL);
        else if (direction == left) motor.turnLeft(currentSpeedR, currentSpeedL);
        rpmL = (pulsePeriodL == 0)? 0 : max(60000000.0 / (pulsePeriodL * WAVES_PER_ROTATION), 0);
        rpmR = (pulsePeriodR == 0)? 0 : max(60000000.0 / (pulsePeriodR * WAVES_PER_ROTATION), 0); 
        Serial.println("Speed: " + String(currentSpeedL) + " / " + String(currentSpeedR) + ", RPM: " + String(round(rpmL)) + " / " + String(round(rpmR)) + ", RPM Target: " + String(rpmTarget));
    }
    
    motor.brake();
    enablePID(false);
    reset();
}

void checkForObstacleAhead() {
    if (getIRDistance(sensor2, A1m, A1c) <= 26 && !obstacleAhead && !avoiding) {
        Serial.println("Detected obstacle ahead.");
        obstacleAhead = true;
        wavesL = 0;
        wavesR = 0;
        wavesLimit = round(WAVES_PER_GRID * 1.9);
    }
}

int getIRDistance(char sensor, double m, double c) {
    int index = -1;
    
    switch (sensor) {
        case sensor1: index = 0; break;
        case sensor2: index = 1; break;
        case sensor3: index = 2; break;
        case sensor4: index = 3; break;
        case sensor5: index = 4; break;
        case sensor6: index = 5; break;
    }

    if (index == -1) return -1;
    int raw = analogRead(sensor);
    sensorValues[index][sensorCounter[index]] = raw;
    sensorCounter[index]++;
    if (sensorCounter[index] >= SENSOR_ROLLING_AVERAGE_COUNT) sensorCounter[index] = 0;

    int average = 0;
    int count = 0;
    
    for (int i = 0; i < SENSOR_ROLLING_AVERAGE_COUNT; i++) {
        int value = sensorValues[index][i];
        
        if (value > 0) {
            average += value;
            count++;
        }
    }

    average = (average / count);
    double volts = map(raw, 0, 1023, 0, 5000) / 1000.0;
    int dist = round((1 / (volts * m + c)) - ((index < 5)? 1.32 : 1.02));

    if (index == 1) {
        int t10 = sensorRaw[index];
        if (dist <= 20 && average >= t10) dist = 10;
        if (sensorDistance[index] == 10 && dist <= 20) return 10;
    }
    
    if ((dist > 25) && (index < 5 || (index == 5 && dist <= 48))) dist += 1.5; 
    dist = abs(dist);
    sensorDistance[index] = dist;
    //Serial.println("Average Reading = " + String(average));
    return dist;
}

void E1() {
    pulseTimeNowL = micros();
    pulsePeriodL = (pulseTimeNowL - pulseTimeLastL);
    pulseTimeLastL = pulseTimeNowL;
    wavesL++;
    
    if (wavesL == wavesLimit) {
        moveEnabled = false;
        motor.brake();
    }
}

void E2() {
    pulseTimeNowR = micros();
    pulsePeriodR = (pulseTimeNowR - pulseTimeLastR);
    pulseTimeLastR = pulseTimeNowR;
    wavesR++;
    
    if (wavesR == wavesLimit) {
        moveEnabled = false;
        motor.brake();
    }
}

boolean isNumber(String str) {
    for (byte i = 0; i < str.length(); i++) {
        if (!isDigit(str.charAt(i))) return false;
    }

    return true;
}
