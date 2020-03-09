#include <EnableInterrupt.h>
#include "Motor.h"
#include "LPS.h"
#include "PID.h"
#include "Sensors.h"
#include "WallHug.h"

// 2.89
// 4.591
const double TICKS_PER_MM = 3.06;
const double TICKS_PER_ANGLE = 4.58;
const int EXPLORE_SPEED = 340;
const int FAST_SPEED = 340;

volatile boolean movingLeft = false;
volatile boolean movingRight = false;
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
boolean runningAlgo = false;

Motor motor;
Sensors sensors;
LPS lps(&ticksLeft, &ticksRight, TICKS_PER_MM);
PID pid(&error, &setpoint, 60.0, 10.0, 400.0);
WallHug wallHug;

void setup() {   
    motor.init();
    wallHug.init();
    enableInterrupt(ENCODER_LEFT, interruptLeft, CHANGE);
    enableInterrupt(ENCODER_RIGHT, interruptRight, CHANGE);
    Serial.begin(115200);
    align();
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
            char command = inputString.charAt(0);
            executeCommand(command, 100);            
        } else if (size > 1) {
            char command = 'A';
            int counter = 0;
                
            for (int i = 0; i < size; i++) {
                char x = inputString.charAt(i);    
                boolean last = (i == size - 1);

                if (x == command) {
                    counter++;
                    if (last) executeCommand(command, 100 * counter);
                } else {
                    if (last) executeCommand(command, 100 * counter);
                    counter = 1;                    
                    command = x;
                    delay(50);
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
        case 'E':
            speedMax = EXPLORE_SPEED;
            fast = false;
            Serial.println("Slow");
            break;
        case 'F':
            speedMax = FAST_SPEED;
            fast = true;
            Serial.println("Fast");
            break;
        case 'W':
            startWallHug();
            break;
    }
}

void move(int direction, int distance) {    
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
            ticksTarget = distance * TICKS_PER_MM;
            break;
        case LEFT:
        case RIGHT:
            ticksTarget = distance * TICKS_PER_ANGLE;
            break;
        default: return;
    };
    
    int speedLeftRef = speedMax;
    int speedRightRef = speedMax - 30;
    motor.move(direction, speedLeftRef, speedRightRef);
    int counter = 0;
    boolean accelerating = true;
    boolean decelerating = false;
    double lastLoopTime = millis();

    while (movingLeft && movingRight) {   
        if (millis() - lastLoopTime < 2) continue;
        lastLoopTime = millis();  

        if (direction == FORWARD) {
            if (sensors.isObstructedFront()) break;
            else if (sensors.isNearFront() && !fast) decelerating = true;
        }

        if ((ticksTarget - ticksLeft <= 144 || ticksTarget - ticksRight <= 144)) decelerating = true;       
        error = lps.computeError();   
//        Serial.println(error);
        double speedOffset = pid.computeOffset();   
        int speedLeft = round(speedLeftRef - speedOffset);
        speedLeft = constrain(speedLeft, speedLeftRef - 100, speedLeftRef + 100);
        int speedRight = round(speedRightRef + speedOffset);
        speedRight = constrain(speedRight, speedRightRef - 100, speedRightRef + 100);
        motor.setSpeed(speedLeft, speedRight);

        // if (accelerating) {
        //     if (speedLeftRef < speedMax) counter++;
        //     else accelerating = false;

        //     if (counter >= 1) {
        //         counter = 0;
        //         speedLeftRef += 20;
        //         speedRightRef += 20;
        //     }
        // } else 
        
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
    movingLeft = false;
    movingRight = false;
    
    if (!fast) {        
        if (direction <= REVERSE && (ticksLeft >= 88 || ticksRight >= 88)) moved = 1;
        delay(10);
        align(); 
        delay(100);
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

    while (movingLeft || movingRight) {   
        if (millis() - lastLoopTime < 1) continue;
        lastLoopTime = millis();  

        double error = (front) ? sensors.getErrorFront() : sensors.getErrorLeft();
        
        if (direction == RIGHT && error >= lowerBound) break;
        else if (direction == LEFT && error <= upperBound) break;
        else if (direction == FORWARD && sensors.getDistanceAverageFront() <= upperBound) break;
        else if (direction == REVERSE && sensors.getDistanceAverageFront() >= lowerBound) break;
        
//        error = lps.computeError();    
//        double speedOffset = pid.computeOffset();   
//        int speedLeft = round(speedLeftRef - speedOffset);
//        speedLeft = constrain(speedLeft, speedLeftRef - 50, speedLeftRef + 50);
//        int speedRight = round(speedRightRef + speedOffset);
//        speedRight = constrain(speedRight, speedRightRef - 50, speedRightRef + 50);
//        motor.setSpeed(speedLeft, speedRight);
        counter++;
        if (counter >= 500) break;
    }

    motor.brake();
    movingLeft = false;
    movingRight = false;
}

void align() {      
    if (sensors.mayAlignFront()) {
        alignFront();
    } else if (sensors.mayAlignLeft()) {
        alignLeft();  
    }
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
        if (distance < 5) moveAlign(REVERSE, true, 4.5, 5.5);
        else if (distance > 6) moveAlign(FORWARD, true, 4.5, 5.5);
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

    if (runningAlgo) {
        if (step == 1) wallHug.moveForward();
        wallHug.updateSensor1(s1);
        wallHug.updateSensor2(s2);
        wallHug.updateSensor3(s3);
        wallHug.updateSensor4(s4);
        wallHug.updateSensor5(s5);
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


/* ALGORITHMS */
void startWallHug() {
    runningAlgo = true;
    int moveCounter = 0;
    String pastActions = "";
    boolean hasMoved = false;

    while (true) {
        if (Serial.available()) {
            char input = (char) Serial.read();
            if (input == 'B') break;
        }

        if (hasMoved && wallHug.isBackAtStart()) break;

        if (pastActions == "LMMLMMLMMLMM") {
            moveCounter = 0;
            pastActions = "";
            wallHug.turn(90);
            move(RIGHT, 90);
            delay(10);
            continue;
        }

        if (wallHug.isLeftMovable() && moveCounter >= 2) {
            moveCounter = 0;
            pastActions += 'L';
            wallHug.turn(-90);
            move(LEFT, 90);
            hasMoved = true;
            delay(10);
            continue;
        }

        if (wallHug.isFrontMovable()) {
            moveCounter++;
            if (moveCounter > 2) pastActions = "";
            else pastActions += 'M';
            move(FORWARD, 100);
            hasMoved = true;
            delay(10);
            continue;
        }

        moveCounter = 0;
        pastActions = "";
        wallHug.turn(90);
        move(RIGHT, 90);
        hasMoved = true;
        delay(10);
        continue;
    }

    Serial.write(80);
    Serial.write(70);
    Serial.write(10);
    runningAlgo = false;
}
