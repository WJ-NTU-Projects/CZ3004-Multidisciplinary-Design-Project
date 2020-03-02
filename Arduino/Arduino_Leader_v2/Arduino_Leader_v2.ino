#include "Arduino_Leader_v2.h"

void setup() {
    pinMode(ENCODER_LEFT, INPUT);
    pinMode(ENCODER_RIGHT, INPUT);
    digitalWrite(ENCODER_LEFT, HIGH);       
    digitalWrite(ENCODER_RIGHT, HIGH);       
    enableInterrupt(ENCODER_LEFT, interruptLeft, CHANGE);
    enableInterrupt(ENCODER_RIGHT, interruptRight, CHANGE);
    Serial.begin(115200);
    align();
}

void loop() {    
    if (Serial.available()) {
        char command = char(Serial.read());

        switch (command) {
            case 'E':
                speedLeftRef = EXPLORE_SPEED_LEFT;
                speedRightRef = EXPLORE_SPEED_LEFT - 30;
                break;
            case 'F':
                speedLeftRef = FAST_SPEED_LEFT;
                speedRightRef = FAST_SPEED_LEFT - 30;
                break;
            case 'I':
                printSensorValues(0);
                break;  
            case 'M':
                move(FORWARD, 100);
                postMove();
                break; 
            case 'L':
                move(LEFT, 90);
                postMove();
                break;
            case 'R':
                move(RIGHT, 90);
                postMove();
                break;
            case 'V':
                move(REVERSE, 100);
                postMove();
                break; 
        }
    }
}

void postMove() {
    delay(10);
    align();
    delay(10);
    printSensorValues(0);
}

void move(int direction, double distance) {    
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    motor.move(direction, speedLeftRef, speedRightRef);
    double lastLoopTime = millis();

    while (true) {   
        if (millis() - lastLoopTime < 10) continue;
        lastLoopTime = millis();  
        double travelled = 0;

        switch (direction) {
            case FORWARD:
                lps.computePosition();
                travelled = lps.getX();
                if (sensors.isObstructedFront() || travelled - 10 >= distance) break;
                break;
            case REVERSE:
                lps.computePosition();
                travelled = lps.getX();
                if (travelled - 10 >= distance) break;
                break;
            case LEFT:
                lps.computeLeftTurn();
                travelled = lps.getHeading();
                if (travelled == distance) break;
                break;
            case RIGHT:
                lps.computeRightTurn();
                travelled = lps.getHeading();
                if (travelled == distance) break;
                break;
        }

        double speedOffset = 0;
        
        if (sensors.mayAlignLeft()) {
            error = -(sensors.getErrorLeft());       
            speedOffset = leftAlignPID.computeOffset();   
        } else {
            error = lps.getY();            
            speedOffset = pid.computeOffset();     
        }
        
        speedLeft = round(speedLeftRef - speedOffset);
        speedLeft = constrain(speedLeft, speedLeftRef - 50, speedLeftRef + 50);
        speedRight = round(speedRightRef + speedOffset);
        speedRight = constrain(speedRight, speedRightRef - 50, speedRightRef + 50);
        motor.setSpeed(speedLeft, speedRight);
    }

    motor.brake();
}

void align() {    
    if (sensors.mayAlignLeft()) alignLeft();    
    else if (sensors.mayAlignFront()) alignFront();
}

void alignLeft() {
    double alignCounter = 0;

    while (alignCounter < 50) {
        double error = sensors.getErrorLeft();
        double lowerBound = 0.05;
        double upperBound = 0.25;
        
        if (error >= lowerBound && error <= upperBound) return;
        move((error < lowerBound) ? RIGHT : LEFT, 0.2);
        alignCounter++;
        delay(5);
    }
}

void alignFront() {    
    double alignCounter = 0;
    
    while (alignCounter < 50) {
        double error = sensors.getErrorFront();
        double lowerBound = -0.1;
        double upperBound = 0.1;
        
        if (error >= lowerBound && error <= upperBound) return;
        move((error < lowerBound) ? RIGHT : LEFT, 0.2);
        alignCounter++;
        delay(5);
    }
}

void printSensorValues(int step) {
    int s1 = sensors.getPrintDistance(1);
    int s2 = sensors.getPrintDistance(2);
    int s3 = sensors.getPrintDistance(3);
    int s4 = sensors.getPrintDistance(4);
    int s5 = sensors.getPrintDistance(5);
    int s6 = sensors.getPrintDistance(6);
    Serial.write(80);
    Serial.print(s1);
    Serial.write(35);
    Serial.print(s2);
    Serial.write(35);
    Serial.print(s3);
    Serial.write(35);
    Serial.print(s4);
    Serial.write(35);
    Serial.print(s5);
    Serial.write(35);
    Serial.print(s6);
    Serial.write(10);
    Serial.flush();
}

void interruptLeft() {
    ticksLeft += 0.5;
}

void interruptRight() {
    ticksRight += 0.5;
}
