#include "Arduino_Leader_v3.h"

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
                speedMax = EXPLORE_SPEED;
                Serial.println("Speed set to EXPLORE");
                break;
            case 'F':
                speedMax = FAST_SPEED;
                Serial.println("Speed set to FAST");
                break;
            case 'I':
                printSensorValues(0);
                break;  
            case 'Z':
                Serial.println(sensors.getErrorLeft());
                Serial.println(sensors.getErrorFront());
                Serial.println(sensors.getDistanceAverageFront());
                break;  
            case 'M':
                move(FORWARD, 100);
                postMove();
                break; 
            case 'N':
                move(FORWARD, 2000);
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
            case 'T':
                move(RIGHT, 180);
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
    align();
    printSensorValues(0);
}

void move(int direction, double distance) {    
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    movingLeft = true;
    movingRight = true;
    ticksTarget = (direction == FORWARD || direction == REVERSE) ? ticksTarget = distance * TICKS_PER_MM : distance * TICKS_PER_ANGLE;
    
    int speedLeftRef = 100;
    int speedRightRef = 70;
    motor.move(direction, speedLeftRef, speedRightRef);
    double lastLoopTime = millis();
    int counter = 0;
    boolean accelerating = true;
    boolean decelerating = false;

    while (movingLeft && movingRight) {   
        if (millis() - lastLoopTime < 10) continue;
        lastLoopTime = millis();  
        
        if (direction == FORWARD) {
            if (sensors.isObstructedFront()) break;         
            else if (sensors.isNearFront()) decelerating = true;
        }

        if (ticksTarget - ticksLeft <= 289 || ticksTarget - ticksRight <= 289) decelerating = true;
        lps.computePosition();     
        error = lps.getY();       
        double speedOffset = pid.computeOffset();   
        speedLeft = round(speedLeftRef - speedOffset);
        speedLeft = constrain(speedLeft, speedLeftRef - 50, speedLeftRef + 50);
        speedRight = round(speedRightRef + speedOffset);
        speedRight = constrain(speedRight, speedRightRef - 50, speedRightRef + 50);
        motor.setSpeed(speedLeft, speedRight);

        if (accelerating) {
            if (speedLeftRef < speedMax) counter++;
            else accelerating = false;
            
            if (counter >= 3) {
                counter = 0;
                speedLeftRef += 20;
                speedRightRef += 20;
            }
        } else if (decelerating) {
            if (speedLeftRef > 140) counter++;

            if (counter >= 2) {
                counter = 0;
                speedLeftRef -= 20;
                speedRightRef -= 20;
            }
        }
    }

    motor.brake();
    movingLeft = false;
    movingRight = false;
}

void moveAlign(int direction, double lowerBound, double upperBound) {    
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    movingLeft = true;
    movingRight = true;
    ticksTarget = 99999999;
    
    int speedLeftRef = 90;
    int speedRightRef = 60;
    motor.move(direction, speedLeftRef, speedRightRef);
    
    double lastLoopTime = millis();

    while (movingLeft && movingRight) {   
        if (millis() - lastLoopTime < 1) continue;
        lastLoopTime = millis();  
        
        if (direction == RIGHT && sensors.getErrorLeft() >= lowerBound) break;
        else if (direction == LEFT && sensors.getErrorLeft() <= upperBound) break;
        else if (direction == FORWARD && sensors.getDistanceAverageFront() <= upperBound) break;
        else if (direction == REVERSE && sensors.getDistanceAverageFront() >= lowerBound) break;
        
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
    movingLeft = false;
    movingRight = false;
}

void align() {      
    if (sensors.mayAlignFront()) {
        alignFront();
        if (sensors.mayAlignLeft()) alignLeft();
    }
    else if (sensors.mayAlignLeft()) alignLeft();  
}

void alignLeft() {
    double error = sensors.getErrorLeft();
    double lower = -0.1;
    double upper = 0.1;
    
    if (abs(error) <= 15) {
        if (error < lower) moveAlign(RIGHT, lower, upper);
        else if (error > upper) moveAlign(LEFT, lower, upper);
    } 
}

void alignFront() {    
    double error = sensors.getErrorFront();
    

    if (abs(error) <= 3) {
        double distance = sensors.getDistanceAverageFront();
        if (distance < 5) moveAlign(REVERSE, 5, 6);
        else if (distance > 6) moveAlign(FORWARD, 5, 6);
    }    

    error = sensors.getErrorFront();
    
    if (abs(error) <= 15) {
        if (error < -0.1) moveAlign(RIGHT, -0.1, 0.1);
        else if (error > 0.1) moveAlign(LEFT, -0.1, 0.1);
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
    if (!movingLeft) return;
    ticksLeft += 0.5;
    
    if (abs(ticksLeft - ticksTarget) <= 0.25) {
        motor.brakeLeft(400);
        movingLeft = false;
    }
}

void interruptRight() {
    if (!movingRight) return;
    ticksRight += 0.5;

    if (abs(ticksRight - ticksTarget) <= 0.25) {
        motor.brakeRight(400);
        movingRight = false;
    }
}
