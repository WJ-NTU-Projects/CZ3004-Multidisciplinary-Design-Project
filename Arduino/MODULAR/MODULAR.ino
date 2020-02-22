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

/**
 * Does ONE of three things per loop.
 */
void loop() {
    // If robot is moving or turning. 
    if (moving || turning) {
        moveProgress();
        if (sensor.hasObstacleFront(10)) brake();
        return;
    }

    // If there is movement discrepancy after moving forward.
    if (positionXError > 0) {        
        move(FORWARD, positionXError);
        return;
    }

    // If robot moved with last 2 seconds, attempt to align if there are long obstacles within 20cm.
    if (millis() - lastMoveTime < 2000 && !aligning) {
        if (sensor.mayAlignFront()) alignFront();
        else if (sensor.mayAlignLeft()) alignLeft();
        return;
    }
}

void moveProgress() {       
    //if (!newTick) return;
    //newTick = false;

    // Compute the estimated position of the robot using some voodoo math only when robot is moving forward / reverse.
    // X is the axis for going straight (distance travelled).
    // Y is how much the robot deviated from the straight line.
    // Heading is the angle the robot is facing.
    if (currentDirection == FORWARD) {
        position.compute();
        positionX = position.getX();
        positionY = position.getY();
        heading = position.getHeading();
    }

    // If either motor is not moving, return.
    // There is no need to perform speed adjustments if one motor has stopped.
    // Motors brake individually --> scroll to bottom to see the interrupt routine.
    if (!movingLeft || !movingRight) {
        
        // If both motors aren't moving, calculate position discrepancy based on difference between voodoo math and ticks if robot was moving forward / reverse.
        // Then call brake which resets a bunch of stuffs.
        if (!movingLeft && !movingRight) {
            if (currentDirection == FORWARD) positionXError = round(distanceTarget - positionX);
            brake();
        }
        
        return;
    }

    // If y-offset < 0.5 (relatively straight), increase speed by 10, constrainted to MAX_SPEED defined in header.
    if (abs(positionY) < 0.5) {
        speedLeft += 10;
        speedRight += 10;
        motor.setSpeed(constrain(speedLeft, 1, MAX_SPEED), constrain(speedRight, 1, MAX_SPEED));
    }

    // Get some ticks going first.
    delay(10);
    
    // Compute speed offset. 
    // Increase speed on the left if y-offset is positive (straying left) and decrease speed on the right.
    // Opposite if y-offset is negative (straying right).
    // Speed is constrained to MAX_SPEED + 50.
    pid.compute();
    speedLeft = (positionY > 0)? speedLeft + speedOffset : speedLeft - speedOffset;
    speedRight = (positionY > 0)? speedRight - speedOffset : speedRight + speedOffset;
    motor.setSpeed(constrain(speedLeft, 1, MAX_SPEED + 50), constrain(speedRight, 1, MAX_SPEED + 50));
}

void move(int direction, int distance) {
    // Resets a bunch of stupid variables, then set the reference variables for direction and distance.
    reset();
    currentDirection = direction;
    distanceTarget = distance;

    // Ticks calculation depending on move or turn.
    // Note that ticks are multiplied by 100 such that math is done in integer (more efficient).
    // Distance is in millimeters while angle is in degrees, but they share the same variable.
    if (direction == FORWARD || direction == REVERSE) {
        ticksTarget = (distance + round(positionY)) * TICKS_PER_MM;
        moving = true;
    } else {
        ticksTarget = TICKS_PER_ANGLE * (distance - heading);
        turning = true;
    }

    // Flags for both motor interrupts.
    movingLeft = true;
    movingRight = true;

    // Reset distance discrepancy if not black magic happens.
    positionXError = 0;
}

void alignFront() {
    aligning = true;
    int directionBeforeAlign = currentDirection;
    double distance1 = sensor.getSensorDistance1(COARSE);
    double distance3 = sensor.getSensorDistance3(COARSE);
    
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
        heading = 0;
        
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
    double distance4 = sensor.getSensorDistance4(COARSE);
    double distance5 = sensor.getSensorDistance5(COARSE);
    
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
            heading = 0;
            if (distanceError > 0.5) move(LEFT, 1);
            else move(RIGHT, 1);
        }
    }
    
    currentDirection = directionBeforeAlign;
    aligning = false;
}

void serialEvent() {
    String inputString = "";  
    int counter = 0;
    
    while (true) {
        if (Serial.available()) {
            char inChar = (char) Serial.read();
            if (inChar == '\n') break;        
            inputString += inChar;
        }

        counter++;
        if (counter >= 2000) break;
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
        String divider = ", ";
        Serial.println(s1 + divider + s2 + divider + s3 + divider + s4 + divider + s5 + divider + s6);
        return;
    }
}

void brake() {
    lastMoveTime = millis();
    moving = false;
    turning = false;
    motor.brake();
    delay(10);
    reset();
    Serial.println("MOVE_FINISHED");
}

void reset() {
    ticksLeft = 0;
    ticksRight = 0;
    speedLeft = 0;
    speedRight = 0;
    speedOffset = 0;
    position.reset();
    pid.reset();
}

boolean isNumber(String str) {
    for (byte i = 0; i < str.length(); i++) if (!isDigit(str.charAt(i))) return false;
    return true;
}


/**
 * Interrupts are called every time there is a change in pin input: HIGH -> LOW / LOW -> HIGH
 * It is theoretically called every half of a square wave.
 * As such, ticks are incremented by 50 per interrupt. (Ticks are multiplied by 100 for integer math, so 0.5 --> 50.)
 */
 
void interruptLeft() {
    if (!movingLeft) return;
    ticksLeft += 50;   
    //newTick = true;

    // If ticks from left encoder >= ticksTarget, brake left motor.
    if (ticksLeft >= ticksTarget) {
        motor.brakeLeftOnly();
        movingLeft = false;
    }
}

void interruptRight() {    
    if (!movingRight) return;
    ticksRight += 50;   
    //newTick = true;

    // If ticks from right encoder >= ticksTarget, brake right motor.
    if (ticksRight >= ticksTarget) {
        motor.brakeRightOnly();
        movingRight = false;
    }
}
