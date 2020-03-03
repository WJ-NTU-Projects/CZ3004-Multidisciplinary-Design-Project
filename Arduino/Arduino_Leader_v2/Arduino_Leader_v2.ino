#include "Arduino_Leader_v2.h"

void setup() {
    pinMode(ENCODER_LEFT, INPUT);
    pinMode(ENCODER_RIGHT, INPUT);
    digitalWrite(ENCODER_LEFT, HIGH);       
    digitalWrite(ENCODER_RIGHT, HIGH);       
    enableInterrupt(ENCODER_LEFT, interruptLeft, CHANGE);
    enableInterrupt(ENCODER_RIGHT, interruptRight, CHANGE);
    Serial.begin(115200);
//    leftErrorReference = 0;
//    
//    for (int i = 0; i < 50; i++) {
//        leftErrorReference += sensors.getErrorLeft();
//    }
//
//    leftErrorReference *= 0.02;
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
                started = true;
                move(FORWARD, 100);
                postMove();
                break; 
            case 'L':
                started = true;
                move(LEFT, 90);
                postMove();
                break;
            case 'R':
                started = true;
                move(RIGHT, 90);
                postMove();
                break;
            case 'V':
                started = true;
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
    moving = true;

    if (direction == FORWARD || direction == REVERSE) {
        ticksTarget = distance * TICKS_PER_MM;
    } else {
        ticksTarget = distance * TICKS_PER_ANGLE;
    }

    
    motor.move(direction, speedLeftRef, speedRightRef);
    double lastLoopTime = millis();

    while (moving) {   
        if (millis() - lastLoopTime < 10) continue;
        lastLoopTime = millis();  
        
        if (direction == FORWARD && sensors.isObstructedFront()) {
            motor.brake();
            moving = false;
            break;         
        }

        lps.computePosition();     
        error = lps.getY();         
        double speedOffset = pid.computeOffset();   
        
        speedLeft = round(speedLeftRef - speedOffset);
        speedLeft = constrain(speedLeft, speedLeftRef - 50, speedLeftRef + 50);
        speedRight = round(speedRightRef + speedOffset);
        speedRight = constrain(speedRight, speedRightRef - 50, speedRightRef + 50);
        motor.setSpeed(speedLeft, speedRight);
    }

    motor.brake();
    moving = false;
    started = false;
}

void moveAlign(int direction) {    
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    motor.move(direction, 120, 90);
    double lastLoopTime = millis();
    int distance = 0.1;
    ticksTarget = 99999999;
    moving = true;

    while (true) {   
        if (millis() - lastLoopTime < 10) continue;
        lastLoopTime = millis();  
        double travelled = 0;
        switch (direction) {
            case LEFT:
                lps.computeLeftTurn();
                travelled = lps.getHeading();
                
                if (travelled >= distance) {
                    motor.brake();
                    return;
                }
                
                break;
            case RIGHT:
                lps.computeRightTurn();
                travelled = lps.getHeading();
                
                if (travelled >= distance) {
                    motor.brake();
                    return;
                }
                
                break;
        }

        double speedOffset = 0;        
        error = lps.getY();            
        setpoint = 0;
        speedOffset = pid.computeOffset();   
        
        speedLeft = round(120 - speedOffset);
        speedLeft = constrain(speedLeft, 120 - 50, 120 + 50);
        speedRight = round(90 + speedOffset);
        speedRight = constrain(speedRight, 90 - 50, 90 + 50);
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
        moveAlign((error < lowerBound) ? RIGHT : LEFT);
        alignCounter++;
        delay(5);
    }
//    while (alignCounter < 50) {
//        double error = sensors.getErrorLeft();
//        if (error < leftErrorReference) moveAlign(RIGHT);
//        else if (error > leftErrorReference) moveAlign(LEFT);
//        else return;
//        alignCounter++;
//        delay(5);
//    }
}

void alignFront() {    
    double alignCounter = 0;
    
    while (alignCounter < 50) {
        double error = sensors.getErrorFront();
        double lowerBound = -0.1;
        double upperBound = 0.1;
        
        if (error >= lowerBound && error <= upperBound) return;
        moveAlign((error < lowerBound) ? RIGHT : LEFT);
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
    if (!moving) return;
    ticksLeft += 0.5;
    
    if (abs(ticksLeft - ticksTarget) <= 0.25) {
        motor.brake();
        moving = false;
    }
}

void interruptRight() {
    if (!moving) return;
    ticksRight += 0.5;

    if (abs(ticksRight - ticksTarget) <= 0.25) {
        motor.brake();
        moving = false;
    }
}
