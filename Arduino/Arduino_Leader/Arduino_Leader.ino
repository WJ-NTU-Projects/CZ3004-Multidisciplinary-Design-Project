#include "Arduino_Leader.h"

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
            case '\0':
            case '\n':
                break;
            case 'E':
                speedLeft = EXPLORE_SPEED_LEFT;
                speedRight = EXPLORE_SPEED_LEFT - 30;
                break;
            case 'F':
                speedLeft = FAST_SPEED_LEFT;
                speedRight = FAST_SPEED_LEFT - 30;
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
            case 'C':
                align();
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

    switch (direction) {
        case FORWARD:
            motor.forward(speedLeft, speedRight);
            break;
        case REVERSE:
            motor.reverse(speedLeft, speedRight);
            break;
        case LEFT:
            motor.turnLeft(speedLeft, speedRight);
            break;
        case RIGHT:
            motor.turnRight(speedLeft, speedRight);
            break;
        default: return;
    }

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

        double error = 0;
        double speedOffset = 0;
        
        if (sensors.mayAlignLeft()) {
            error = -(sensors.getErrorLeft());       
            speedOffset = leftAlignPID.computeOffset();   
        } else {
            error = lps.getY();            
            speedOffset = pid.computeOffset();     
        }
        
        double newSpeedLeft = speedLeft - speedOffset;
        newSpeedLeft = constrain(newSpeedLeft, speedLeft - 50, speedLeft + 50);
        double newSpeedRight = speedRight + speedOffset;
        newSpeedRight = constrain(newSpeedRight, speedRight - 50, speedRight + 50);
        motor.setSpeed(newSpeedLeft, newSpeedRight);
    }

    motor.brakeRight();
    motor.brakeLeft();
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
    int s1 = ceil(sensors.getDistanceR(1) * 0.1);
    int s2 = ceil(sensors.getDistanceR(2) * 0.1);
    int s3 = ceil(sensors.getDistanceR(3) * 0.1);
    int s4 = ceil(sensors.getDistanceR(4) * 0.1);
    int s5 = ceil(sensors.getDistanceR(5) * 0.1);
    int s6 = ceil(sensors.getDistanceR(6) * 0.1);
    Serial.print('P');
    Serial.print(s1);
    Serial.print('#');
    Serial.print(s2);
    Serial.print('#');
    Serial.print(s3);
    Serial.print('#');
    Serial.print(s4);
    Serial.print('#');
    Serial.print(s5);
    Serial.print('#');
    Serial.println(s6);
    Serial.flush();
}

void interruptLeft() {
    ticksLeft += 0.5;
}

void interruptRight() {
    ticksRight += 0.5;
}