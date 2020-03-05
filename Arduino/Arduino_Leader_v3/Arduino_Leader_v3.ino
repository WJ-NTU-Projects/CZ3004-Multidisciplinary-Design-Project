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
    String inputString = "";

    if (Serial.available()) {
        while (true) {
            char input = (char) Serial.read();
            if (input == '\r') continue;
            if (input == '\n') break;
            inputString += input;
        }
        
        int size = inputString.length();

        if (size == 1) {
            char command = inputString.charAt(0);

            switch (command) {
                case 'I': printSensorValues(0); break;  
                case 'M': move(FORWARD, 100); break; 
                case 'L': move(LEFT, 90); break;
                case 'R': move(RIGHT, 90); break;
                case 'T': move(RIGHT, 180); break;
                case 'V': move(REVERSE, 100); break; 
                case 'C': align(); break;
            }
        } else if (size > 1) {
            char command = 'A';
            int counter = 0;
            currentSpeedLeft = 100;
            currentSpeedRight = 70;
                
            for (int i = 0; i < size; i++) {
                char x = inputString.charAt(i);    

                if (x == command) {
                    counter++;

                    if (i == size - 1) {
                        if (command == 'M') moveContinuous(FORWARD, 100 * counter);
                        else if (command == 'L') moveContinuous(LEFT, 90);
                        else if (command == 'R') moveContinuous(RIGHT, 90);
                        return;
                    }
                } else {
                    if (command == 'M') moveContinuous(FORWARD, 100 * counter);
                    else if (command == 'L') moveContinuous(LEFT, 90);
                    else if (command == 'R') moveContinuous(RIGHT, 90);
                    counter = 1;                    
                    command = x;
                    delay(5);
                }
            }
        }

        inputString = "";
    }
}

void move(int direction, double distance) {    
    speedMax = EXPLORE_SPEED;
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    continuousMovement = false;
    movingLeft = true;
    movingRight = true;

    switch (direction) {
        case FORWARD:
        case REVERSE:
            ticksTarget = distance * TICKS_PER_MM;
            break;
        case LEFT:
        case RIGHT:
            ticksTarget = distance * TICKS_PER_ANGLE;
            break;
        default: return;
    };
    
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
        error = lps.computeError();   
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
    delay(5);
    align();
    delay(5);
    align();
    delay(10);
    printSensorValues(0);
}

void moveAlign(int direction, boolean front, double lowerBound, double upperBound) {    
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    continuousMovement = false;
    movingLeft = true;
    movingRight = true;
    ticksTarget = 99999999;
    
    int speedLeftRef = 100;
    int speedRightRef = 70;
    motor.move(direction, speedLeftRef, speedRightRef);
    double lastLoopTime = millis();
    int counter = 0;

    while (movingLeft || movingRight) {   
        if (millis() - lastLoopTime < 1) continue;
        lastLoopTime = millis();  

        double error = (front) ? sensors.getErrorFront() : sensors.getErrorLeft();
        
        if (direction == RIGHT && error >= lowerBound) break;
        else if (direction == LEFT && error <= upperBound) break;
        else if (direction == FORWARD && sensors.getDistanceAverageFront() <= upperBound) break;
        else if (direction == REVERSE && sensors.getDistanceAverageFront() >= lowerBound) break;
        
        error = lps.computeError();    
        double speedOffset = pid.computeOffset();   
        speedLeft = round(speedLeftRef - speedOffset);
        speedLeft = constrain(speedLeft, speedLeftRef - 50, speedLeftRef + 50);
        speedRight = round(speedRightRef + speedOffset);
        speedRight = constrain(speedRight, speedRightRef - 50, speedRightRef + 50);
        motor.setSpeed(speedLeft, speedRight);
        counter++;
        if (counter >= 500) break;
    }

    motor.brake();
    movingLeft = false;
    movingRight = false;
}

void moveContinuous(int direction, double distance) {   
    speedMax = FAST_SPEED; 
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    continuousMovement = true;
    movingLeft = true;
    movingRight = true;
    int speedLeftRef = currentSpeedLeft;
    int speedRightRef = currentSpeedRight;

    switch (direction) {
        case FORWARD:
        case REVERSE:
            ticksTarget = (distance - 100) * TICKS_PER_MM;
            break;
        case LEFT:
            ticksTarget = distance * TICKS_PER_ANGLE;
            speedLeftRef = 0;
            break;
        case RIGHT:
            ticksTarget = distance * TICKS_PER_ANGLE;
            speedRightRef = 0;
            break;
        default: return;
    };
    
    motor.move(direction, speedLeftRef, speedRightRef);
    double lastLoopTime = millis();
    int counter = 0;
    boolean accelerating = true;
    boolean decelerating = false;

    while (movingLeft && movingRight) {   
        if (millis() - lastLoopTime < 2) continue;
        lastLoopTime = millis();  
        
        if (direction == FORWARD) {
            if (sensors.isObstructedFront()) {
                motor.brake();
                movingLeft = false;
                movingRight = false;
                return;         
            } else if (sensors.isNearFront()) {
                decelerating = true;
            }
        }

        if (ticksTarget - ticksLeft <= 289 || ticksTarget - ticksRight <= 289) decelerating = true;
        error = lps.computeError();   
        double speedOffset = pid.computeOffset();   
        speedLeft = round(speedLeftRef - speedOffset);
        speedLeft = constrain(speedLeft, speedLeftRef - 50, speedLeftRef + 50);
        speedRight = round(speedRightRef + speedOffset);
        speedRight = constrain(speedRight, speedRightRef - 50, speedRightRef + 50);
        if (direction == LEFT) speedLeft = 0;
        else if (direction == RIGHT) speedRight = 0;
        motor.setSpeed(speedLeft, speedRight);

        if (accelerating) {
            if (speedLeftRef < speedMax) counter++;
            else accelerating = false;
            
            if (counter >= 10) {
                counter = 0;
                speedLeftRef += 20;
                speedRightRef += 20;
            }
        } else if (decelerating) {
            if (speedLeftRef > 140) counter++;

            if (counter >= 10) {
                counter = 0;
                speedLeftRef -= 20;
                speedRightRef -= 20;
            }
        }
    }

    movingLeft = false;
    movingRight = false;
    currentSpeedLeft = speedLeftRef;
    currentSpeedRight = speedRightRef;
}

void align() {      
    if (sensors.mayAlignFront()) alignFront();
    else if (sensors.mayAlignLeft()) alignLeft();  
}

void alignLeft() {
    double error = sensors.getErrorLeft();
    double lower = -0.1;
    double upper = 0.1;
    
    if (abs(error) <= 15) {
        if (error < lower) moveAlign(RIGHT, false, lower, upper);
        else if (error > upper) moveAlign(LEFT, false, lower, upper);
    } 
}

void alignFront() {    
    double error = sensors.getErrorFront();
    
    if (abs(error) <= 3) {
        double distance = sensors.getDistanceAverageFront();
        if (distance < 5) moveAlign(REVERSE, true, 5, 6);
        else if (distance > 6) moveAlign(FORWARD, true, 5, 6);
    }    
    
    error = sensors.getErrorFront();

    if (abs(error) <= 15) {
        if (error < -0.1) moveAlign(RIGHT, true, -0.1, 0.1);
        else if (error > 0.1) moveAlign(LEFT, true, -0.1, 0.1);
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
        if (!continuousMovement) motor.brakeLeft(400);
        movingLeft = false;
    }
}

void interruptRight() {
    if (!movingRight) return;
    ticksRight += 0.5;

    if (abs(ticksRight - ticksTarget) <= 0.25) {
        if (!continuousMovement) motor.brakeRight(400);
        movingRight = false;
    }
}
