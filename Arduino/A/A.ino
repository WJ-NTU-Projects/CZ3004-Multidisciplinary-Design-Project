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
    if (sensor.mayAlignLeft()) alignLeft();
}

void loop() {
    if (millis() - loopTime < 10) return;
    loopTime = millis();

//    Serial.println(sensor.getSensorErrorLeft());
//    delay(500);
//    return;
    
    if (moving) {
        if (!movingLeft && !movingRight) {
            eBrake();
            return;
        }

        if (aligning) {
            double error = (aligningFront) ? sensor.getSensorErrorFront() : sensor.getSensorErrorLeft();

            if (alignCounter < 10) {
                alignCounter++;
            } else {
                eBrake();
                alignCounter = 0;
                return;
            }
            
            if (aligningFront && error >= -0.3 && error <= -0.1) {
                eBrake();            
                return;
            }
            
            if (aligningLeft && error >= 0.2 && error <= 0.4) {
                eBrake();            
                return;
            }

            
            return;
        }    
    
        if (currentDirection == LEFT || currentDirection == RIGHT) {
            lps.computePosition();    
            localX = lps.getX();
            localY = lps.getY();
            speedOffset = pid.computeOffset();
            applyPID();
            return;
        }

//        double distance4 = sensor.getSensorDistance(sensor4, A3m, A3c, A3r);
//        double distance5 = sensor.getSensorDistance(sensor5, A4m, A4c, A4r);

        lps.computePosition();    
        localX = lps.getX();
        localY = lps.getY();
        speedOffset = pid.computeOffset();
        applyPID();

        if (localX - localRef >= 100) {
            Serial.println("Travelled one grid.");
            localRef = floor(localX * 0.01) * 100;

//            if (distance4 > 10 && distance5 > 10 && !leftEmpty && localX >= 200) {
//                eBrake();
//                delay(10);
//                move(LEFT, 90);
//                leftEmpty = true;
//                return;
//            }

            if (sensor.mayAlignLeft()) {
                double error = sensor.getSensorErrorLeft();
                
                if (fabs(error) < 5) {
                    if (error < 0.2) {
                        setpoint = 0.5;
                    } else if (error > 0.4) {
                        setpoint = -0.5;
                    } else {
                        setpoint = 0;
                    }
                }
            } else if (sensor.mayAlignFront()) {
                setpoint = 0;
                return;
                double error = sensor.getSensorErrorFront();
                
                if (fabs(error) < 5) {
                    if (error < -0.3) {
                        setpoint = -0.1;
                    } else if (error > 0.1) {
                        setpoint = 0.1;
                    } else {
                        setpoint = 0;
                    }
                }
            } else {
                setpoint = 0;
            }
        }
        
        return;
    }


    // NOT MOVING
    
    if (aligning) {
        aligning = false;

        if (alignCounter < 10) {
            delay(10);
            if (aligningFront) alignFront();
            else if (aligningLeft) alignLeft();
            alignCounter++;
            return;
        }

        alignCounter = 0;
        aligningFront = false;
        aligningLeft = false;
    } else {
        if (millis() - lastMoveTime > 250) return;
        double errorLeft = sensor.getSensorErrorLeft();
        double errorFront = sensor.getSensorErrorFront();
        if (sensor.mayAlignLeft()) alignLeft();
        //else if (sensor.mayAlignFront()) alignFront();
        if (aligning) return;
    }
    
//    if (sensor.hasObstacleFront(6) || sensor2Close) {
//        if (sensor.hasObstacleLeft(10)) move(RIGHT, 90);
//        else move(LEFT, 90);
//        return;
//    }
//    
//    move(FORWARD, 2000);
}

void applyPID() {
    double newSpeedLeft = speedLeft - speedOffset;
    newSpeedLeft = constrain(newSpeedLeft, speedDefault - 50, speedDefault + 50);
    double newSpeedRight = speedRight + speedOffset;
    newSpeedRight = constrain(newSpeedRight, speedDefault - 90, speedDefault + 10);
    motor.setSpeed(newSpeedLeft, newSpeedRight);
}

void move(int direction, double distance) {
    if (direction == FORWARD || direction == REVERSE) {
        ticksTarget = distance * TICKS_PER_MM;    
        localX = 0;
        localY = 0;
        speedLeft = speedDefault;
        speedRight = speedDefault - 40;
        leftEmpty = false;
    } else {
        ticksTarget = distance * TICKS_PER_ANGLE;
        speedLeft = EXPLORE_SPEED_LEFT;
        speedRight = speedLeft - 40;
    }

    currentDirection = direction;
    movingLeft = true;
    movingRight = true;
    sensor2Close = false;
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

void moveAlign(int direction) {
    speedLeft = TURN_SPEED_LEFT;
    speedRight = speedLeft - 40;
    ticksTarget = 100000;
    movingLeft = true;
    movingRight = true;
    ticksLeft = 0;
    ticksRight = 0;
    speedOffset = 0;
    pid.reset();
    lps.reset();
    moving = true;
    aligning = true;
    alignStartTime = millis();

    if (direction == RIGHT) motor.turnRight(speedLeft, speedRight);
    else if (direction == LEFT) motor.turnLeft(speedLeft, speedRight);
}

void eBrake() {
    movingLeft = false;
    movingRight = false;
    motor.brakeLeft();
    motor.brakeRight();
    moving = false;
    lastMoveTime = millis();
}

void alignFront() {    
    int directionBeforeAlign = currentDirection;
    double distance1 = sensor.getSensorDistance(sensor1, A0m, A0c, A0r);
    double distance3 = sensor.getSensorDistance(sensor3, A2m, A2c, A2r);
    if (distance1 < 0 || distance3 < 0) return;
    
    double distanceAverage = (distance1 + distance3) * 0.5;
    double distanceError = distance1 - distance3;
    
    if (distanceAverage > 0 && distanceAverage <= 10) {
        heading = 0;
        
        if (distanceError < -0.3) {
            aligningFront = true;
            moveAlign(RIGHT);
        } else if (distanceError > -0.1) {
            aligningFront = true;
            moveAlign(LEFT);
        }
    }

    currentDirection = directionBeforeAlign;
}

void alignLeft() {
        
    int directionBeforeAlign = currentDirection;
    double distance4 = sensor.getSensorDistance(sensor4, A3m, A3c, A3r);
    double distance5 = sensor.getSensorDistance(sensor5, A4m, A4c, A4r);
    if (distance4 < 0 || distance5 < 0) return;
    
    double distanceAverage = (distance4 + distance5) * 0.5;
    double distanceError = distance4 - distance5;
    
    if (distanceAverage > 0 && distanceAverage <= 10) {
        if (distanceError < 0.2) {
            aligningLeft = true;
            moveAlign(LEFT);
        } else if (distanceError > 0.4) {
            aligningLeft = true;
            moveAlign(RIGHT);
        }
    }
    
    currentDirection = directionBeforeAlign;
}

void serialEvent() {
    if (moving) return;
    String inputString = "";  
    unsigned long timeNow = millis();
    
    while (true) {
        if (Serial.available()) {
            char inChar = (char) Serial.read();
            //Serial.println(inChar);
            if (inChar == '\n') break;        
            inputString += inChar;
        }

        if (millis() - timeNow >= 500) break;
    } 

    char input = inputString.charAt(0);

    if (input == 'I') {
        String s1 = String(sensor.getSensorDistance(sensor1, A0m, A0c, A0r));
        String s2 = String(sensor.getSensorDistance(sensor2, A1m, A1c, A1r));
        String s3 = String(sensor.getSensorDistance(sensor3, A2m, A2c, A2r));
        String s4 = String(sensor.getSensorDistance(sensor4, A3m, A3c, A3r));
        String s5 = String(sensor.getSensorDistance(sensor5, A4m, A4c, A4r));
        String s6 = String(sensor.getSensorDistance(sensor6, A5m, A5c, A5r));
        Serial.println("P" + s1 + "#" + s2 + "#" + s3 + "#" + s4 + "#" + s5 + "#" + s6);
        Serial.flush();
        return;   
    }

    if (input == 'M') {
        move(FORWARD, 100); 
        return;
    }

    if (input == 'L') {
        move(LEFT, 90); 
        return;
    }

    if (input == 'R') {
        move(RIGHT, 90); 
        return;
    }

    if (input == 'C') {
            double errorLeft = sensor.getSensorErrorLeft();
            double errorFront = sensor.getSensorErrorFront();
            Serial.println(errorLeft);
            Serial.println(sensor.mayAlignLeft());
            if (sensor.mayAlignLeft() && errorLeft < 0.2 && errorLeft > 0.4) alignLeft();
            else if (sensor.mayAlignFront() && errorFront < -0.3 && errorFront > -0.1) alignFront();
            return;
    }
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
