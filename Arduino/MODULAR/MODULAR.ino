#include "MODULAR.h"

void setup() {
    pinMode(ENCODER_LEFT, INPUT);
    pinMode(ENCODER_RIGHT, INPUT);
    digitalWrite(ENCODER_LEFT, HIGH);       
    digitalWrite(ENCODER_RIGHT, HIGH);       
    enableInterrupt(ENCODER_LEFT, interruptLeft, CHANGE);
    enableInterrupt(ENCODER_RIGHT, interruptRight, CHANGE);
    Serial.begin(115200);
    delay(1000);
}

void loop() {
    if (moving || turning) {
        moveProgress();
        return;
    }

    if (positionXError > 0) {        
        move(FORWARD, positionXError);
        return;
    }

    if (millis() - lastMoveTime < 2000 && !aligning) {
        if (sensor.mayAlignFront()) alignFront();
        else alignLeft();
        return;
    }
}

void moveProgress() {       
    if (!newTick) return;
    newTick = false;
    position.compute();
    positionX = position.getX();
    positionY = position.getY();
    heading = position.getHeading();

    if (!movingLeft || !movingRight) {
        if (!movingLeft && !movingRight) {
            positionXError = round(distanceTarget - positionX);
            brake();
        }
        
        return;
    }

    if (abs(positionY) < 0.5) {
        speedLeft += 10;
        speedRight += 10;
        motor.setSpeed(constrain(speedLeft, 1, MAX_SPEED), constrain(speedRight, 1, MAX_SPEED));
        if (positionY == 0) return;
    }
    
    pid.compute();
    speedLeft = (positionY > 0)? speedLeft + speedOffset : speedLeft - speedOffset;
    speedRight = (positionY > 0)? speedRight - speedOffset : speedRight + speedOffset;
    motor.setSpeed(constrain(speedLeft, 1, MAX_SPEED + 50), constrain(speedRight, 1, MAX_SPEED + 50));
}

// DISTANCE IS IN mm / degree
void move(int direction, int distance) {
    reset();
    currentDirection = direction;
    distanceTarget = distance;
    
    if (direction == FORWARD || direction == REVERSE) {
        ticksTarget = distance * TICKS_PER_MM;
        moving = true;
    } else {
        ticksTarget = TICKS_PER_ANGLE * (distance - heading);
        turning = true;
    }

    movingLeft = true;
    movingRight = true;
}

void brake() {
    lastMoveTime = millis();
    moving = false;
    turning = false;
    motor.brake();
    delay(10);
    reset();
}

void reset() {
    ticksLeft = 0;
    ticksRight = 0;
    speedLeft = 0;
    speedRight = 0;
    speedOffset = 0;
    pid.reset();
}

void alignFront() {
    aligning = true;
    int directionBeforeAlign = currentDirection;
    double distance1 = sensor.getSensorDistance1();
    double distance3 = sensor.getSensorDistance3();
    
    if (distance1 < 0 || distance3 < 0) {
        aligning = false;
        return;
    }
    
    double distanceAverage = (distance1 + distance3) * 0.5;

    if (distanceAverage > 10) {
        aligning = false;
        return;
    }

    if (distance1 < 6 || distance3 < 6) {
        move(REVERSE, 10);
        currentDirection = directionBeforeAlign;
        aligning = false;
        return;
    }

    double distanceError = distance1 - distance3;

    if (distanceAverage > 6 && distanceAverage <= 10) {
        if (abs(distanceError) > 0.5) {
            if (distanceError > 0.5) move(RIGHT, 1);
            else move(LEFT, 1);
        } else {
            int distanceDifference = distanceAverage * 10 - 60; // convert to mm
            if (distanceDifference < 0)  move(REVERSE, abs(distanceDifference));
            else if (distanceDifference > 0) move(FORWARD, abs(distanceDifference));            
        }
    }
    
    currentDirection = directionBeforeAlign;
    aligning = false;
}

void alignLeft() {
    aligning = true;
    int directionBeforeAlign = currentDirection;
    double distance4 = sensor.getSensorDistance4();
    double distance5 = sensor.getSensorDistance5();
    
    if (distance4 < 0 || distance5 < 0) {
        aligning = false;
        return;
    }
    
    double distanceAverage = (distance4 + distance5) * 0.5;

    if (distanceAverage > 20) {
        aligning = false;
        return;
    }

    double distanceError = distance4 - distance5;

    if (distanceAverage > 0 && distanceAverage <= 20) {
        if (abs(distanceError) > 0.5) {
            if (distanceError > 0.5) move(LEFT, 1);
            else move(RIGHT, 1);
        }
    }
    
    currentDirection = directionBeforeAlign;
    aligning = false;
}

void interruptLeft() {
    if (!movingLeft) return;
    ticksLeft += 50;   
    newTick = true;
    
    if (ticksLeft >= ticksTarget) {
        motor.brakeLeftOnly();
        movingLeft = false;
    }
}

void interruptRight() {    
    if (!movingRight) return;
    ticksRight += 50;   
    newTick = true;

    if (ticksRight >= ticksTarget) {
        motor.brakeRightOnly();
        movingRight = false;
    }
}
