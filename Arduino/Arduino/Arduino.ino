#include "Arduino.h"

void setup() {
    pinMode(ENCODER_LEFT, INPUT);
    pinMode(ENCODER_RIGHT, INPUT);
    digitalWrite(ENCODER_LEFT, HIGH);       
    digitalWrite(ENCODER_RIGHT, HIGH);       
    enableInterrupt(ENCODER_LEFT, interruptLeft, CHANGE);
    enableInterrupt(ENCODER_RIGHT, interruptRight, CHANGE);
    Serial.begin(115200);
    //align();
}

void loop() {    
    if (Serial.available()) {
        char command = char(Serial.read());

        switch (command) {
            case '\0':
            case '\n':
                break;
                
            case 'E':
                automate = true;
                fast = false;
                break;
                
            case 'F':
                automate = true;
                fast = true;
                break;
                
            case 'I':
                printSensorValues(0);
                break;  
                
            case 'M':
                move(FORWARD, 100);
                align();
                delay(50);
                printSensorValues(0);
                Serial.write(80);
                Serial.write(77);
                Serial.write(10);
                break; 
                 
            case 'L':
                move(LEFT, 90);
                align();
                delay(50);
                printSensorValues(0);
                Serial.write(80);
                Serial.write(76);
                Serial.write(10);
                break;
                
            case 'R':
                move(RIGHT, 90);
                align();
                delay(50);
                printSensorValues(0);
                Serial.write(80);
                Serial.write(82);
                Serial.write(10);
                break;
                
            case 'C':
                align();
                Serial.write(80);
                Serial.write(67);
                Serial.write(10);
                break; 
        }
    }
}

void move(int direction, double distance) {    
    double speedLeft = (fast) ? FAST_SPEED_LEFT : EXPLORE_SPEED_LEFT;
    double speedRight = speedLeft - 30;
    if (direction == FORWARD || direction == REVERSE) ticksTarget = distance * TICKS_PER_MM;
    else if (direction == LEFT) ticksTarget = distance * TICKS_PER_ANGLE_L;
    else if (direction == RIGHT) ticksTarget = distance * TICKS_PER_ANGLE_R;

    localX = 0;
    localY = 0;
    movingLeft = true;
    movingRight = true;
    ticksLeft = 0;
    ticksRight = 0;
    speedOffset = 0;
    pid.reset();
    lps.reset();
    moving = true;

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
        default:
            return;
    }

    double lastLoopTime = millis();
    double counter = 0;
    int localRef = 0;

    while (movingLeft || movingRight) {   
        if (millis() - lastLoopTime < 10) continue;
        lastLoopTime = millis();                

        if (Serial.available()) {
            char command = char(Serial.read());
            
            if (command == 'B') {
                brake();
                break; 
            }
        }
        
        lps.computePosition();
        localX = lps.getX();
        localY = lps.getY();
        speedOffset = pid.computeOffset();       

        if (direction == FORWARD) {
            int distance2 = sensors.getDistanceR(2);
            int distance1 = sensors.getDistanceR(1);
            int distance3 = sensors.getDistanceR(3);
            
            if (distance2 > 0 && distance2 <= 5) {
                brake();
                break;
            } else if (distance1 > 0 && distance1 <= 5) {
                brake();
                break;
            } else if (distance3 > 0 && distance3 <= 5) {
                brake();
                break;
            } 
        
//            int difference = localX - localRef;
//            
//            if (difference >= 90 && difference <= 110) {
//                localRef = round(localX * 0.01);
//                printSensorValues(localRef);
//                localRef *= 100;
//
////                if (automate && sensors.getDistanceR(4) > 12 && sensors.getDistanceR(5) > 12) {
////                    brake();
////                    break;
////                } 
//            }
        }

        double newSpeedLeft = speedLeft - speedOffset;
        newSpeedLeft = constrain(newSpeedLeft, speedLeft - 50, speedLeft + 50);
        double newSpeedRight = speedRight + speedOffset;
        newSpeedRight = constrain(newSpeedRight, speedRight - 50, speedRight + 50);
        motor.setSpeed(newSpeedLeft, newSpeedRight);
        counter++;
    }

    moving = false;
}

void brake() {
    motor.brakeLeft();
    motor.brakeRight();
    movingLeft = false;
    movingRight = false;
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
    char set0 = 48;
    if (step >= 0 && step <= 9) set0 += step;
    else if (step >= 10) set0 = 65 + step - 10;
    else return;
    
    int sensor1 = sensors.getDistanceR(1);
    int sensor2 = sensors.getDistanceR(2);
    int sensor3 = sensors.getDistanceR(3);
    int sensor4 = sensors.getDistanceR(4);
    int sensor5 = sensors.getDistanceR(5);
    int sensor6 = sensors.getDistanceR(6);
    sensor1 = (sensor1 > 0 && sensor1 <= 10) ? 1 : 0;
    sensor2 = (sensor2 > 0 && sensor2 <= 10) ? 1 : 0;
    sensor3 = (sensor3 > 0 && sensor3 <= 10) ? 1 : 0;
    sensor4 = (sensor4 > 0 && sensor4 <= 10) ? 1 : 0;
    sensor5 = (sensor5 > 0 && sensor5 <= 10) ? 1 : 0;
    sensor6 = (sensor6 > 0 && sensor6 <= 10) ? 1 : 0;

    char set1 = 48 + (sensor1 * 4 + sensor2 * 2 + sensor3 * 1);
    char set2 = 48 + (sensor4 * 4 + sensor5 * 2 + sensor6 * 1);
    Serial.write(80);
    //Serial.write(set0);
    Serial.write(set1);
    Serial.write(set2);
    Serial.write(10);
}

void interruptLeft() {
    if (!movingLeft) return;
    ticksLeft += 0.5;
    
    if (abs(ticksLeft - ticksTarget) <= 0.25) {
        motor.brakeLeft();
        movingLeft = false;
    }
}

void interruptRight() {
    if (!movingRight) return;
    ticksRight += 0.5;

    if (abs(ticksRight - ticksTarget) <= 0.25) {
        motor.brakeRight();
        movingRight = false;
    }
}
