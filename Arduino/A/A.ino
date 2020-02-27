#include "A.h"

void setup() {
    pinMode(ENCODER_LEFT, INPUT);
    pinMode(ENCODER_RIGHT, INPUT);
    digitalWrite(ENCODER_LEFT, HIGH);       
    digitalWrite(ENCODER_RIGHT, HIGH);       
    enableInterrupt(ENCODER_LEFT, interruptLeft, CHANGE);
    enableInterrupt(ENCODER_RIGHT, interruptRight, CHANGE);
    Serial.begin(115200);
}

void loop() {
    if (Serial.available()) {
        char command = char(Serial.read());

        switch (command) {
            case '\0':
            case '\n':
                break;

            case 'I':
                printSensorValues();
                break;

            case 'M':
                move(FORWARD, 100);
                Serial.println("PM");
                break;

            case 'N':
                move(FORWARD, 2000);
                Serial.println("PN");
                break;                

            case 'L':
                move(LEFT, 90);
                Serial.println("PL");
                break;

            case 'R':
                move(RIGHT, 90);
                Serial.println("PR");
                break;

            case 'C':
                align();
                Serial.println("PC");
                break;
        }
    }
}

void move(int direction, int distance) {    
    double speedLeft = 100;
    double speedRight = speedLeft - 30;
    int speedCap = (fast) ? FAST_SPEED_LEFT : EXPLORE_SPEED_LEFT;
    
    if (direction == FORWARD || direction == REVERSE) {
        ticksTarget = distance * TICKS_PER_MM;
        localX = 0;
        localY = 0;
    } else {
        ticksTarget = distance * TICKS_PER_ANGLE;
        speedCap = EXPLORE_SPEED_LEFT;
    }
    
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
        
    while (movingLeft || movingRight) {   
        if (millis() - lastLoopTime < 10) continue;
        lastLoopTime = millis();                
        lps.computePosition();
        localX = lps.getX();
        localY = lps.getY();
        speedOffset = pid.computeOffset();              

        double newSpeedLeft = speedLeft - speedOffset;
        newSpeedLeft = constrain(newSpeedLeft, speedCap - 50, speedCap + 50);
        double newSpeedRight = speedRight + speedOffset;
        newSpeedRight = constrain(newSpeedRight, speedCap - 80, speedCap + 20);
        motor.setSpeed(newSpeedLeft, newSpeedRight);
        counter++;

        if (speedLeft < speedCap && counter >= 10) {
            speedLeft += 20;
            speedRight += 20;
            counter = 0;
        }
    }

    moving = false;
}

void align() {    
    if (sensors.mayAlignLeft()) alignLeft();    
    else if (sensors.mayAlignFront()) alignFront();
}

void alignLeft() {
    double alignCounter = 0;
    
    while (alignCounter < 20) {
        double error = sensors.getErrorLeft();
        double lowerBound = -0.5;
        double upperBound = 0.5;
        
        if (error >= lowerBound && error <= upperBound) return;
        move((error < lowerBound) ? LEFT : RIGHT, 0.2);
        alignCounter++;
        delay(10);
    }
}

void alignFront() {    
    double alignCounter = 0;
    
    while (alignCounter < 20) {
        double error = sensors.getErrorFront();
        double lowerBound = -0.5;
        double upperBound = 0.5;
        
        if (error >= lowerBound && error <= upperBound) return;
        move((error < lowerBound) ? RIGHT : LEFT, 0.2);
        alignCounter++;
        delay(10);
    }
}

void printSensorValues() {
    String s1 = String(sensors.getDistance(1));
    String s2 = String(sensors.getDistance(2));
    String s3 = String(sensors.getDistance(3));
    String s4 = String(sensors.getDistance(4));
    String s5 = String(sensors.getDistance(5));
    String s6 = String(sensors.getDistance(6));
    Serial.println("P" + s1 + "#" + s2 + "#" + s3 + "#" + s4 + "#" + s5 + "#" + s6);
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
