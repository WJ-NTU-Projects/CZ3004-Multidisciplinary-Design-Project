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
    if (millis() - loopTime < 10) return;
    loopTime = millis();
    
    if (!moving) {
        if (ALIGN_ENABLED && millis() - lastMoveTime < 250) {
            if (sensor.mayAlignFront()) alignFront();
            else if (sensor.mayAlignLeft()) alignLeft();
            return;
        }

        // EVERYTHING BELOW IS DEBUGGING ONLY
        if (!DEBUGGING) return;
        
        if (sensor.hasObstacleFront(10)) {
            return;
            if (sensor.hasObstacleLeft(10)) move(RIGHT, 90);
            else move(LEFT, 90);
            return;
        }
        
        move(FORWARD, 1500);
        return;
    }   

    // MOVING
    
    if (currentDirection == FORWARD || currentDirection == REVERSE) lps.computePosition();    
    localX = lps.getX();
    localY = lps.getY();
    heading = lps.getHeading();

    if (!movingLeft && !movingRight) {
        moving = false;            
        return;
    }

    if (fabs(localY) < 1.0e-6) return;
    
    speedOffset = pid.computeOffset();
    speedLeft += (localY > 0) ? speedOffset : -speedOffset;
    speedRight += (localY > 0) ? -speedOffset : speedOffset;
    speedLeft = constrain(speedLeft, 0, speedMax);
    speedRight = constrain(speedRight, 0, speedMax);
    motor.setSpeed(speedLeft, speedRight);

    if (localX - localRef < 100) return;
    
    switch (globalHeading) {
        case 0  : globalY++; break;
        case 90 : globalX++; break;
        case 180: globalY--; break;
        case 270: globalX--; break;
    } 

    globalX = constrain(globalX, 1, 13);
    globalY = constrain(globalY, 1, 18);
    localRef = floor(localX * 0.01) * 100;
    Serial.println("#robotPosition:" + String(globalX) + VAR_DIV + String(globalY) + VAR_DIV + String(globalHeading));
}

void move(int direction, int distance) {
    if (direction == FORWARD || direction == REVERSE) {
        distance += round(localY);
        ticksTarget = distance * TICKS_PER_MM;    
    } else {
        distance -= heading;
        ticksTarget = distance * TICKS_PER_ANGLE;
        globalHeading += (direction == RIGHT) ? 90 : -90;
        if (globalHeading < 0) globalHeading += 360;
        if (globalHeading >= 360) globalHeading -= 360;
    }

    currentDirection = direction;
    ticksLeft = 0;
    ticksRight = 0;
    speedLeft = (distance > 100) ? speedDefault : SPEED_SLOW;
    speedRight = (distance > 100) ? speedDefault : SPEED_SLOW;
    speedOffset = 0;
    localX = 0;
    localY = 0;
    localRef = 0;
    heading = 0;
    pid.reset();
    lps.reset();
    movingLeft = true;
    movingRight = true;
    moving = true;

    if      (direction == FORWARD)  motor.forward(speedLeft, speedRight);
    else if (direction == REVERSE)  motor.reverse(speedLeft, speedRight);
    else if (direction == LEFT)     motor.turnRight(speedLeft, speedRight);
    else if (direction == RIGHT)    motor.turnLeft(speedLeft, speedRight);
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
    String inputString = "";  
    unsigned long timeNow = millis();
    
    while (true) {
        if (Serial.available()) {
            char inChar = (char) Serial.read();
            if (inChar == '\n') break;        
            inputString += inChar;
        }

        if (millis() - timeNow >= 2000) break;
    } 

    if (inputString.indexOf("EX") == 0) {
        speedDefault = EXPLORE_SPEED;
        speedMax = EXPLORE_SPEED_MAX;
        return;
    }

    if (inputString.indexOf("FP") == 0) {
        speedDefault = FAST_SPEED;
        speedMax = FAST_SPEED_MAX;
        return;
    }

    int distance = 100;
    int angle = 90;
    
    if (inputString.length() > 1) {
        String distanceStr = inputString.substring(1);
        
        if (isNumber(distanceStr)) {
            distance = distanceStr.toInt();
            angle = distance;
        }
    }
 
    if (inputString.indexOf("M") == 0) {
        move(FORWARD, distance);
        return;
    }
    
    if (inputString.indexOf("L") == 0) {
        move(LEFT, angle);
        return;
    }
    
    if (inputString.indexOf("R") == 0) {
        move(RIGHT, angle);
        return;
    }
    
    if (inputString.indexOf("C") == 0) {
        if (sensor.mayAlignFront()) alignFront();
        else alignLeft(); 
        return;
    }

    if (inputString.indexOf("I") == 0) {
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

boolean isNumber(String str) {
    for (byte i = 0; i < str.length(); i++) if (!isDigit(str.charAt(i))) return false;
    return true;
}

void interruptLeft() {
    if (!movingLeft) return;
    ticksLeft += 50;

    if (abs(ticksLeft - ticksTarget) <= 25) {
        motor.brakeLeft();
        movingLeft = false;
        lastMoveTime = millis();
    }
}

void interruptRight() {
    if (!movingRight) return;
    ticksRight += 50;

    if (abs(ticksRight - ticksTarget) <= 25) {
        motor.brakeRight();
        movingRight = false;
        lastMoveTime = millis();
    }
}
