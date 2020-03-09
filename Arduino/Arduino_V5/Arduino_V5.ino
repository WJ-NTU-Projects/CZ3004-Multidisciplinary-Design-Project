#include <EnableInterrupt.h>
#include "Motor.h"
#include "LPS.h"
#include "PID.h"
#include "Sensors.h"

// 2.89
// 4.591
const double TICKS_PER_MM = 3.05;
const double TICKS_PER_ANGLE_L = 4.59;
const double TICKS_PER_ANGLE_R = 4.60;
const int EXPLORE_SPEED = 300;
const int FAST_SPEED = 300;

volatile boolean moving = false;
volatile double ticksLeft = 0;
volatile double ticksRight = 0;
volatile double ticksTarget = 0;
double error = 0;
double setpoint = 0;
int speedMax = EXPLORE_SPEED;
int moved = 0;
String inputString = "";
boolean inputComplete = false;
boolean fast = false;

Motor motor;
Sensors sensors;
LPS lps(&ticksLeft, &ticksRight, TICKS_PER_MM);
PID pid(&error, &setpoint, 60.0, 10.0, 400.0);

void setup() {   
    motor.init();
    enableInterrupt(ENCODER_LEFT, interruptLeft, CHANGE);
    enableInterrupt(ENCODER_RIGHT, interruptRight, CHANGE);
    Serial.begin(115200);
    
//    delay(3000);
//    move(LEFT, 90);
//    align();
//    delay(100);
//    move(LEFT, 90);
//    align();
//    delay(100);
//    move(RIGHT, 90);
//    align();
//    delay(100);
//    move(RIGHT, 90);
//    align();
//    delay(100);
//    
    Serial.println("Ready");
}

void loop() {    
    if (Serial.available()) {
        char input = (char) Serial.read();
        if (input == '\r') return;
        
        if (input == '\n') {
            inputComplete = true;
        } else {
            inputString += input;
            return;
        }
    }
    
    if (inputComplete) {
        int size = inputString.length();

        if (size == 1) {
            fast = false;
            char command = inputString.charAt(0);
            executeCommand(command, 100);            
        } else if (size > 1) {
            fast = true;
            char command = 'A';
            int counter = 0;
                
            for (int i = 0; i < size; i++) {
                char x = inputString.charAt(i);    
                boolean last = (i == size - 1);

                if (x == command) {
                    counter++;
                    if (last) executeCommand(command, 100 * counter);
                } else {
                    executeCommand(command, 100 * counter);
                    counter = 1;                    
                    command = x;
                    delay(10);
                    if (last) executeCommand(command, 100 * counter);
                }
            }
        }

        inputComplete = false;
        inputString = "";
    }
}

void executeCommand(char command, int moveDistance) {
    switch (command) {
        case 'I': 
            printSensorValues(moved); 
            break;  
        case 'J': 
            Serial.println(sensors.getDistance(1)); 
            Serial.println(sensors.getDistance(2)); 
            Serial.println(sensors.getDistance(3)); 
            Serial.println(sensors.getDistance(4)); 
            Serial.println(sensors.getDistance(5)); 
            Serial.println(sensors.getDistance(6)); 
            Serial.println();
            break; 
        case 'M': 
            move(FORWARD, moveDistance); 
            break; 
        case 'L': 
            move(LEFT, 90); 
            break;
        case 'R': 
            move(RIGHT, 90); 
            break;
        case 'T': 
            move(RIGHT, 180); 
            break;
        case 'V': 
            move(REVERSE, moveDistance); 
            break; 
        case 'C': 
            align(); 
            break;
    }
}

void move(int direction, int distance) {    
    if (direction == FORWARD && (sensors.isObstructedFront())) {
        align(); 
        delay(10);
        printSensorValues(0);
        return;
    }
    
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    moved = 0;
    moving = true;

    switch (direction) {
        case FORWARD:
        case REVERSE:
            ticksTarget = distance * TICKS_PER_MM;
            break;
        case LEFT:
            ticksTarget = distance * TICKS_PER_ANGLE_L;
            break;
        case RIGHT:
            ticksTarget = distance * TICKS_PER_ANGLE_R;
            break;
        default: return;
    };

    double distanceLeft = sensors.getDistanceAverageLeft();
    if (distanceLeft < 4.75) setpoint = -10;
    else if (distanceLeft > 5.25 && distanceLeft <= 12) setpoint = 10;
    else setpoint = 0;

    //speedMax = 100;
    int speedLeftRef = speedMax;
    int speedRightRef = speedMax - 30;
    motor.move(direction, speedLeftRef, speedRightRef);
    
    int counter = 0;
    boolean decelerating = false;
    unsigned long lastLoopTime = millis();

    while (moving) {   
        if (millis() - lastLoopTime < 5) continue;
        lastLoopTime = millis();  

        if (direction == FORWARD) {
            if (sensors.isObstructedFront()) break;
            else if (sensors.isNearFront()) decelerating = true;            
        }

        if ((ticksTarget - ticksLeft <= 153 || ticksTarget - ticksRight <= 153)) decelerating = true;       
        error = lps.computeError();   
        double speedOffset = pid.computeOffset();   
        int speedLeft = round(speedLeftRef - speedOffset);
        speedLeft = constrain(speedLeft, speedLeftRef - 100, speedLeftRef + 100);
        int speedRight = round(speedRightRef + speedOffset);
        speedRight = constrain(speedRight, speedRightRef - 100, speedRightRef + 100);
        motor.setSpeed(speedLeft, speedRight);
        
        if (decelerating) {
             if (speedLeftRef > 120) counter++;

             if (counter >= 1) {
                 counter = 0;
                 speedLeftRef -= 20;
                 speedRightRef -= 20;
             }
        }
    }
    
    motor.brake();
    moving = false;

    if (direction <= REVERSE && (ticksLeft >= 90 || ticksRight >= 90)) moved = 1;
    delay(50);
    align(); 
    
    if (!fast) {
        delay(10);
        printSensorValues(moved);
    }
}

void moveAlign(int direction, boolean front, int frontWhich, double lowerBound, double upperBound) {    
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    moving = true;
    ticksTarget = 99999999;
    
    int speedLeftRef = 70;
    int speedRightRef = 70;
    motor.move(direction, speedLeftRef, speedRightRef);
    double lastLoopTime = millis();
    int counter = 0;

    while (moving) {   
        if (millis() - lastLoopTime < 1) continue;
        lastLoopTime = millis();  

        double error = 0;

        if (front) {
            switch (frontWhich) {
                case 0:
                    error = sensors.getErrorFront();
                    break;
                case 1:
                    error = sensors.getErrorFront1();
                    break;
                case 3:
                    error = sensors.getErrorFront3();
                    break;
            }
        } else {
            error = sensors.getErrorLeft();
        }
        
        if (direction == RIGHT && error >= lowerBound) break;
        else if (direction == LEFT && error <= upperBound) break;
        else if (direction == FORWARD && sensors.getDistanceAverageFront() <= upperBound) break;
        else if (direction == REVERSE && sensors.getDistanceAverageFront() >= lowerBound) break;
        counter++;
        if (counter >= 500) break;
    }

    motor.brake();
    moving = false;
}

void moveAlignS(int direction, int sensor, double lowerBound, double upperBound) {    
    if (direction > REVERSE) return;
    ticksLeft = 0;
    ticksRight = 0;
    pid.reset();
    lps.reset();
    moving = true;
    ticksTarget = 99999999;
    
    int speedLeftRef = 100;
    int speedRightRef = 70;
    motor.move(direction, speedLeftRef, speedRightRef);
    double lastLoopTime = millis();
    int counter = 0;

    while (moving) {         
        if (direction == FORWARD) {
            if (sensors.isObstructedFront()) break;      
        }
        
        if (direction == FORWARD && sensors.getDistance(sensor) <= upperBound) break;
        else if (direction == REVERSE && sensors.getDistance(sensor) >= lowerBound) break;
        error = lps.computeError();   
        double speedOffset = pid.computeOffset();   
        int speedLeft = round(speedLeftRef - speedOffset);
        speedLeft = constrain(speedLeft, speedLeftRef - 100, speedLeftRef + 100);
        int speedRight = round(speedRightRef + speedOffset);
        speedRight = constrain(speedRight, speedRightRef - 100, speedRightRef + 100);
        motor.setSpeed(speedLeft, speedRight);
        counter++;
        if (counter >= 500) break;
    }

    motor.brake();
    moving = false;
}

void align() {      
    
    if (sensors.isNearFront() && !fast) {    
        double distance1 = sensors.getDistance(1);
        double distance2 = sensors.getDistance(2);
        double distance3 = sensors.getDistance(3);   
        int smallestSensor = 2;
        double smallestDistance = distance2;

        if (distance1 < distance2) {
            smallestSensor = 1;
            smallestDistance = distance1;
        }
        
        if (distance3 < distance1) {
            smallestSensor = 3;
            smallestDistance = distance3;
        }
        
        double distanceFront = sensors.getDistanceAverageFront();

        if (distanceFront <= 36 && abs(sensors.getErrorFront()) <= 5) {
            alignFront();
            alignFront();
        } 
            
        if (smallestDistance < 4.75) {
            moveAlignS(REVERSE, 2, 4.75, 5.25);
        } else if (smallestDistance > 5.25) {
            moveAlignS(FORWARD, 2, 4.75, 5.25);
        }
    }
    
    double distance1 = sensors.getDistance(1);
    double distance3 = sensors.getDistance(3);
    double distance4 = sensors.getDistance(4);
    double distance5 = sensors.getDistance(5);

    if (distance4 <= 12 && distance5 <= 12 && abs(distance4 - distance5) <= 3) {
        alignLeft();  
        alignLeft();  

        if (abs(sensors.getErrorFront()) > 3) {
            alignFront();
            alignFront();
        }
        return;
    }

    if (distance1 <= 36 && distance3 <= 36 && abs(distance1 - distance3) <= 5) {
        alignFront();
        alignFront();
        return;
    }
}

void alignLeft() {
    double error = sensors.getErrorLeft();
    double lower = -0.2;
    double upper = 0.2;
    if (error < lower) moveAlign(RIGHT, false, 0, lower, upper);
    else if (error > upper) moveAlign(LEFT, false, 0, lower, upper);
}

void alignFront() {   
    double error = sensors.getErrorFront();
    double lower = -0.2;
    double upper = 0.2;
    if (error < lower) moveAlign(RIGHT, true, 0, lower, upper);
    else if (error > upper) moveAlign(LEFT, true, 0, lower, upper);
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
}

void interruptLeft() {
    if (!moving) return;
    ticksLeft += 0.5;
    
    if (abs(ticksLeft - ticksTarget) <= 0.25) {
        motor.brake();
        moving = false;
    }
}

void interruptRight() {
    if (!moving) return;
    ticksRight += 0.5;

    if (abs(ticksRight - ticksTarget) <= 0.25) {
        motor.brake();
        moving = false;
    }
}
