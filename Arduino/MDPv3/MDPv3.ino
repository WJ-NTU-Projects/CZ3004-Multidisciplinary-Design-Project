#include <EnableInterrupt.h>
#include <PID_v1.h>
#include "DeadReckoning.h"
#include "Robot.h"
#include "MDPv3.h"

int checkListEvadeStep = 0;
double checkListTravelled = 0;

void setup() {
    pinMode(ENCODER_LEFT, INPUT);
    pinMode(ENCODER_RIGHT, INPUT);
    digitalWrite(ENCODER_LEFT, HIGH);       
    digitalWrite(ENCODER_RIGHT, HIGH);       
    enableInterrupt(ENCODER_LEFT, interruptRiseL, RISING);
    enableInterrupt(ENCODER_RIGHT, interruptRiseR, RISING);

    Serial.begin(115200);
    pidLeft.SetOutputLimits(-350, 350);  
    pidLeft.SetSampleTime(2);
    pidRight.SetOutputLimits(-350, 350);   
    pidRight.SetSampleTime(2);
    enablePID(false);
    
    delay(1000);
    //move(forward, 1500);
}

void loop() {
    if (moving || turning) {
        deadReckoning();

        if (moving && dir == forward && !aligning && !turning) {
            if (robot.hasObstacleFront()) {
                brake();
                Serial.println("Obstacle...");
                if (dir == forward) alignFront();
                return;
            }
        }
        
        return;
    }
    
    if (millis() - lastMoveTime < 2000 && !moving && !turning && !aligning) {
        if (robot.hasObstacleFront()) alignFront();
        else alignLeft();
        return;
    }

    move(forward, 1500);
}

void move(Direction direction, double speedR, double speedL) {
    if (direction == forward) robot.moveForward(speedR, speedL);
    else if (direction == reverse) robot.moveBackward(speedR, speedL);
    else if (direction == right) robot.turnRight(speedR, speedL);
    else if (direction == left) robot.turnLeft(speedR, speedL);
}

void move(Direction direction, double distance) {
    reset();
    
    if (direction == forward || direction == reverse) {
        distanceTarget = distance;
        moving = true;
    } else {
        distanceTarget = 1500 * distance * 0.001;
        turning = true;
    }
    
    dir = direction;
    enablePID(true);
    pidLeft.Compute();
    pidRight.Compute();
    if (direction == forward) robot.moveForward(speedR, speedL);
    else if (direction == reverse) robot.moveBackward(speedR, speedL);
    else if (direction == right) robot.turnRight(speedR, speedL);
    else if (direction == left) robot.turnLeft(speedR, speedL);
}

void brake() {
    lastMoveTime = millis();
    moving = turning = false;
    robot.brake();
    enablePID(false);
    delay(50);
    reset();
}

void enablePID(boolean a) {
    pidLeft.SetMode(a? AUTOMATIC : MANUAL);
    pidRight.SetMode(a? AUTOMATIC : MANUAL);
}

void reset() {
    distanceTarget = ticksL = ticksR = rpmL = rpmR = speedL = speedR = 0;
    rpmTargetL = rpmTargetR = 60;
    distanceTravelled = 0;
    dr.reset();
    lastRiseL = lastRiseR = micros();
}

void deadReckoning() {
    if (millis() - lastComputeTime > 25) {
        dr.computePosition();
        lastComputeTime = millis();
        double x = dr.getX();
        double y = dr.getY();
        distanceTravelled = x;
        
            Serial.println(x);
        Serial.println(y);
        
        if (x >= distanceTarget - (moving? 10 : 8)) {
            Serial.println(x);
            brake();   
            return;
        }

        //double maxRpm = (x >= distanceTarget - (moving? 150 : 50))? 45 : (x >= distanceTarget - (moving? 75 : 25))? 30 : 60;
        double maxRpm = 60;

        if (y < 0) {
            rpmTargetR = maxRpm;
            rpmTargetL = maxRpm - 1;
        } else if (y > 0) {
            rpmTargetL = maxRpm;
            rpmTargetR = maxRpm - 1;
        } else {
            rpmTargetL = maxRpm;
            rpmTargetR = maxRpm;
        }
//
        
    }
}

void alignFront() {
    aligning = true;
    dirBeforeAlign = dir;
    double distance1 = robot.getSensorDistance(1);
    double distance3 = robot.getSensorDistance(3);
    if (distance1 < 0 || distance3 < 0) return;
    double distanceAverage = (distance1 + distance3) * 0.5;

    if (distanceAverage > 10) {
        aligning = false;
        return;
    }
    
    if (distanceAverage < 6) {
        move(reverse, 10);
        dir = dirBeforeAlign;
        aligning = false;
        return;
    }
    
    double distanceError = distance1 - distance3;

    if (distanceAverage > 0 && distanceAverage <= 10 && abs(distanceError) > 0.4) {
        if (distanceError > 0.4) {
            move(right, 0.01);
        } else if (distanceError < -0.4) {
            move(left, 0.01);
        }
    }

    delay(20);

    distance1 = robot.getSensorDistance(1);
    distance3 = robot.getSensorDistance(3);
    distanceAverage = (distance1 + distance3) * 0.5;

    if (distanceAverage > 6 && distanceAverage <= 10) {
        int distanceDifference = distanceAverage - 6;

        if (distanceDifference < 0) {
            move(reverse, abs(distanceDifference * 10));
            delay(100);
        } else if (distanceDifference > 0) {
            move(forward, abs(distanceDifference * 10));
            delay(100);
        }
    }

    dir = dirBeforeAlign;
    aligning = false;
}

void alignLeft() {
    aligning = true;
    dirBeforeAlign = dir;
    double distance4 = robot.getSensorDistance(5);
    double distance5 = robot.getSensorDistance(4);
    if (distance4 < 0 || distance5 < 0) return;
    double distanceAverage = (distance4 + distance5) * 0.5;    
    double distanceError = distance4 - distance5;
    Serial.println(distance4);
    Serial.println(distance5);
    
    if (distanceAverage > 0 && distanceAverage <= 30 && abs(distanceError) > 0.1) {
        if (distanceError > 0.4) {
            move(left, 0.01);
        } else if (distanceError < -0.4) {
            move(right, 0.01);
        }
    }

    delay(20);
    dir = dirBeforeAlign;
    aligning = false;
}





// INTERRUPT ROUTINES

void interruptRiseL() {
    if (!moving && !turning) return;
    ticksL += 1;    
    unsigned long timeNow = micros();
    unsigned long period = timeNow - lastRiseL;
    if (period < 0) period = UNSIGNED_LONG_MAX - lastRiseL + timeNow;
    lastRiseL = timeNow;
    rpmL = (period == 0)? 0 : max(60000000.0 / (period * 562.25), 0);
    if (rpmL == 0) return;
    pidLeft.Compute();
    move(dir, max(speedR, 0), max(speedL, 0));
}

void interruptRiseR() {
    if (!moving && !turning) return;
    ticksR += 1;    
    unsigned long timeNow = micros();
    unsigned long period = timeNow - lastRiseR;
    if (period < 0) period = UNSIGNED_LONG_MAX - lastRiseR + timeNow;
    lastRiseR = timeNow;
    rpmR = (period == 0)? 0 : max(60000000.0 / (period * 562.25), 0);
    if (rpmR == 0) return;
    pidRight.Compute();
    move(dir, max(speedR, 0), max(speedL, 0));
}
