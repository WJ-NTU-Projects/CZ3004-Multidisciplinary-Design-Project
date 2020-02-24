#include "A.h"

void setup() {
    pinMode(ENCODER_LEFT, INPUT);
    pinMode(ENCODER_RIGHT, INPUT);
    digitalWrite(ENCODER_LEFT, HIGH);       
    digitalWrite(ENCODER_RIGHT, HIGH);       
    enableInterrupt(ENCODER_LEFT, interruptLeft, CHANGE);
    enableInterrupt(ENCODER_RIGHT, interruptRight, CHANGE);
    Serial.begin(115200);
    delay(1000);
    move(FORWARD, 400);
}

void loop() {
    if (millis() - loopTime < 10) return;
    loopTime = millis();
    
    if (!moving) {
        if (ALIGN_ENABLED && millis() - lastMoveTime < 250) {
            if (sensor.mayAlignFront()) alignFront();
            else if (sensor.mayAlignLeft()) alignLeft();
            return;
        }

        if (test) {
            test = false;
            delay(1000);
            move(RIGHT, 90);
            return;
        }

        if (test2) {
            test2 = false;
            delay(1000);
            move(LEFT, 90);
        }
        return;
    }   
    
    if (currentDirection == FORWARD || currentDirection == REVERSE) {
        lps.computePosition();    
        localX = lps.getX();
        localY = lps.getY();
        heading = lps.getHeading();
    }
    
    if (!movingLeft && !movingRight) {
        moving = false;         
        return;
    }

    speedOffset = pid.computeOffset();
    double newSpeedLeft = speedLeft - speedOffset;
    newSpeedLeft = constrain(newSpeedLeft, 1, 400);
    double newSpeedRight = speedRight + speedOffset;
    newSpeedRight = constrain(newSpeedRight, 1, 400);
    motor.setSpeed(newSpeedLeft, newSpeedRight);
    Serial.println(localY);
}

void move(int direction, int distance) {
    if (direction == FORWARD || direction == REVERSE) {
        if (currentDirection != FORWARD) distance += round(localY);
        ticksTarget = distance * TICKS_PER_MM;    
        localX = 0;
        localY = 0;
        heading = 0;
        speedLeft = speedDefaultLeft;
        speedRight = speedDefaultRight;
    } else {
        if (currentDirection == FORWARD) distance -= heading;
        ticksTarget = distance * TICKS_PER_ANGLE;
        globalHeading += (direction == RIGHT) ? 90 : -90;
        if (globalHeading < 0) globalHeading += 360;
        if (globalHeading >= 360) globalHeading -= 360;
        speedLeft = EXPLORE_SPEED_LEFT;
        speedRight = EXPLORE_SPEED_RIGHT;
    }

    currentDirection = direction;
    movingLeft = true;
    movingRight = true;
    ticksLeft = 0;
    ticksRight = 0;
    speedOffset = 0;
    localRef = 0;
    pid.reset();
    lps.reset();
    moving = true;

    if      (direction == FORWARD)  motor.forward(speedLeft, speedRight);
    else if (direction == REVERSE)  motor.reverse(speedLeft, speedRight);
    else if (direction == RIGHT)    motor.turnRight(speedLeft, speedRight);
    else if (direction == LEFT)     motor.turnLeft(speedLeft, speedRight);
}

void alignFront() {
    int directionBeforeAlign = currentDirection;
    double distance1 = sensor.getSensorDistance1(COARSE);
    double distance3 = sensor.getSensorDistance3(COARSE);
    if (distance1 < 0 || distance3 < 0) return;
    
    double distanceAverage = (distance1 + distance3) * 0.5;
    if (distanceAverage > 10) return;

    if (distance1 < 6 || distance3 < 6) {
        move(REVERSE, 10);
        currentDirection = directionBeforeAlign;
        return;
    }

    double distanceError = distance1 - distance3;

    if (distanceAverage > 6 && distanceAverage <= 10) {
        heading = 0;
        
        if (abs(distanceError) > 0.3) {
            if (distanceError > 0.3) move(RIGHT, 1);
            else move(LEFT, 1);
        } else {
            int distanceDifference = distanceAverage * 10 - 60; // convert to mm
            if (distanceDifference < 0)  move(REVERSE, abs(distanceDifference));
            else if (distanceDifference > 0) move(FORWARD, abs(distanceDifference));            
        }
    }
    
    currentDirection = directionBeforeAlign;
}

void alignLeft() {
    int directionBeforeAlign = currentDirection;
    double distance4 = sensor.getSensorDistance4(COARSE);
    double distance5 = sensor.getSensorDistance5(COARSE);
    if (distance4 < 0 || distance5 < 0) return;
    
    double distanceAverage = (distance4 + distance5) * 0.5;
    if (distanceAverage > 20) return;

    double distanceError = distance4 - distance5;

    if (distanceAverage > 0 && distanceAverage <= 20) {
        if (abs(distanceError) > 0.3) {
            heading = 0;
            if (distanceError > 0.3) move(LEFT, 1);
            else move(RIGHT, 1);
        }
    }
    
    currentDirection = directionBeforeAlign;
}

void serialEvent() {
    if (!Serial.available()) return;
    char input = (char) Serial.read();

    switch (input) {
        case 'M':    
            if (!moving) move(FORWARD, 100); 
            return;
        
                
        case 'L':    
            if (!moving) move(LEFT, 90); 
            return;
                
        case 'R':    
            if (!moving) move(RIGHT, 90); 
            return;
        
        case 'C':    
            if (sensor.mayAlignFront()) alignFront();
            else alignLeft(); 
            return;
                
        case 'I':    
            String s1 = String(sensor.getSensorDistance1(FINE));
            String s2 = String(sensor.getSensorDistance2(FINE));
            String s3 = String(sensor.getSensorDistance3(FINE));
            String s4 = String(sensor.getSensorDistance4(FINE));
            String s5 = String(sensor.getSensorDistance5(FINE));
            String s6 = String(sensor.getSensorDistance6(FINE));
            Serial.println(s1 + VAR_DIV + s2 + VAR_DIV + s3 + VAR_DIV + s4 + VAR_DIV + s5 + VAR_DIV + s6);
            return;    
    }
}

void interruptLeft() {
    if (!movingLeft) return;
    ticksLeft += 0.5;
    
    if (abs(ticksLeft - ticksTarget) <= 0.25) {
        motor.brakeLeft();
        movingLeft = false;
        lastMoveTime = millis();
    }
}

void interruptRight() {
    if (!movingRight) return;
    ticksRight += 0.5;

    if (abs(ticksRight - ticksTarget) <= 0.25) {
        motor.brakeRight();
        movingRight = false;
        lastMoveTime = millis();
    }
}
