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
            if (Serial.available()) {
                char input = (char) Serial.read();
                if (input == '\r') continue;
                if (input == '\n') break;
                inputString += input;
            }
        }
        
        int size = inputString.length();

        if (size == 1) {
            char command = inputString.charAt(0);

            switch (command) {
                case 'I': 
                    printSensorValues(moved); 
                    break;  
                case 'M': 
                    speedMax = EXPLORE_SPEED;
                    move(FORWARD, 100, false); 
                    break; 
                case 'L': 
                    speedMax = EXPLORE_SPEED;
                    move(LEFT, 90, false); 
                    break;
                case 'R': 
                    speedMax = EXPLORE_SPEED;
                    move(RIGHT, 90, false); 
                    break;
                case 'T': 
                    speedMax = EXPLORE_SPEED;
                    move(RIGHT, 180, false); 
                    break;
                case 'V': 
                    speedMax = EXPLORE_SPEED;
                    move(REVERSE, 100, false); 
                    break; 
                case 'C': 
                    align(); 
                    break;
            }
        } else if (size > 1) {
            char command = 'A';
            int counter = 0;
            speedMax = FAST_SPEED;
                
            for (int i = 0; i < size; i++) {
                char x = inputString.charAt(i);    
                boolean last = (i == size - 1);

                if (x == command) {
                    counter++;

                    if (last) {
                        if (command == 'M') move(FORWARD, 100 * counter, true);
                        else if (command == 'V') move(REVERSE, 100 * counter, true);
                        else if (command == 'L') move(LEFT, 90, true);
                        else if (command == 'R') move(RIGHT, 90, true);
                        else if (command == 'T') move(RIGHT, 180, true);
                        return;
                    }
                } else {
                    if (command == 'M') move(FORWARD, 100 * counter, true);
                    else if (command == 'V') move(REVERSE, 100 * counter, true);
                    else if (command == 'L') move(LEFT, 90, true);
                    else if (command == 'R') move(RIGHT, 90, true);
                    else if (command == 'T') move(RIGHT, 180, true);
                    counter = 1;                    
                    command = x;
                    delay(20);

                    if (last) {
                        if (command == 'M') move(FORWARD, 100 * counter, true);
                        else if (command == 'V') move(REVERSE, 100 * counter, true);
                        else if (command == 'L') move(LEFT, 90, true);
                        else if (command == 'R') move(RIGHT, 90, true);
                        else if (command == 'T') move(RIGHT, 180, true);
                    }
                }
            }
        }

        inputString = "";
    }
}

void move(int direction, double distance, boolean fast) {    
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    moved = 0;
    movingLeft = true;
    movingRight = true;

    switch (direction) {
        case FORWARD:
        case REVERSE:
            ticksTarget = distance * ((fast) ? TICKS_PER_MM_FAST : TICKS_PER_MM);
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
            if (sensors.isObstructedFront()) {
                motor.brake();
                movingLeft = false;
                movingRight = false;
                if (ticksLeft >= 87 || ticksRight >= 87) moved = 1;
                align();
                align();
                delay(20);
                printSensorValues(moved);
                return;
            } else if (sensors.isNearFront()) decelerating = true;
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
    if (direction == FORWARD || direction == REVERSE) moved = 1;
    if (!fast) {
    delay(10);
    align();
    delay(10);
    align();
    delay(20);
    printSensorValues(moved);
    }
}

void moveAlign(int direction, boolean front, double lowerBound, double upperBound) {    
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    movingLeft = true;
    movingRight = true;
    ticksTarget = 99999999;
    
    int speedLeftRef = 100;
    int speedRightRef = 70;
    motor.move(direction, speedLeftRef, speedRightRef);
    double lastLoopTime = millis();
    int counter = 0;

    while (movingLeft && movingRight) {   
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

void align() {      
    if (sensors.mayAlignFront()) alignFront();
    else if (sensors.mayAlignLeft()) alignLeft();  
}

void alignLeft() {
    double error = sensors.getErrorLeft();
    double lower = -0.2;
    double upper = 0.2;
    
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
    Serial.write(48 + s1);
    Serial.write(35);
    Serial.write(48 + s2);
    Serial.write(35);
    Serial.write(48 + s3);
    Serial.write(35);
    Serial.write(48 + s4);
    Serial.write(35);
    Serial.write(48 + s5);
    Serial.write(35);
    Serial.write(48 + s6);
    Serial.write(35);
    Serial.write(48 + step);
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
