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
}

void loop() {
    if (!aligned) align();    
//    debugRun();
//    return;

    while (Serial.available() > 0) {
        char input = (char) Serial.read();
        if (input == '\n') break;
        if (input == 'I') printSensorValues();
        else if (input == 'M') move(FORWARD, 100);
        else if (input == 'L') turn(LEFT, 90);
        else if (input == 'R') turn(RIGHT, 90);
        else if (input == 'C') align();
    }
}

void debugRun() {
    if (!sensors.hasObstacleLeft(12)) {
        delay(100);
        turn(LEFT, 90);
    } else if (sensors.hasObstacleFront(7) || sensor2Close) {
        delay(100);
        turn(RIGHT, 90);
        
        if (sensors.hasObstacleFront(7)) {
            delay(100);
            turn(RIGHT, 90);
        }
    }
        
    delay(100);
    move(FORWARD, 100);
}

void move(int direction, int distance) {
    if (direction != FORWARD && direction != REVERSE) return;
    ticksTarget = distance * TICKS_PER_MM;
    localX = 0;
    localY = 0;
    speedLeft = speedDefault;
    speedRight = speedDefault - 30;;
    aligned = false;
    resetMoveVars();

    if (direction == FORWARD)  motor.forward(speedLeft, speedRight);
    else motor.reverse(speedLeft, speedRight);

    while (movingLeft || movingRight) {   
        if (sensors.getDistanceR(2) <= 15 && !sensor2Close) sensor2Close = true;   
             
        if (sensors.hasObstacleFront(7)) {
            eBrake();
            return;
        }

        double setpointOffset = 0.1;
        
        if (sensors.mayAlignLeft()) {
            double error = sensors.getErrorLeft();
            if (error < 0.2) setpoint = setpointOffset;
            else if (error > 0.4) setpoint = -setpointOffset;
            else setpoint = 0; 
        } else {
            if (sensors.mayAlignFront()) {
                double error = sensors.getErrorFront();                
                if (error < -0.3) setpoint = -setpointOffset;
                else if (error > -0.1) setpoint = setpointOffset;
                else setpoint = 0; 
            } else {
                setpoint = 0;
            }
        }
        
        lps.computePosition();
        localX = lps.getX();
        localY = lps.getY();
        speedOffset = pid.computeOffset();
        applyPID();
        delay(5);
    }

    eBrake();
}

void turn(int direction, double angle) {
    if (direction != LEFT && direction != RIGHT) return;
    ticksTarget = angle * TICKS_PER_ANGLE;
    speedLeft = EXPLORE_SPEED_LEFT;
    speedRight = speedLeft - 30;
    resetMoveVars();
    
    if (direction == RIGHT) motor.turnRight(speedLeft, speedRight);
    else motor.turnLeft(speedLeft, speedRight);

    while (movingLeft || movingRight) {
        delay(5);        
        lps.computePosition();
        localX = lps.getX();
        localY = lps.getY();
        speedOffset = pid.computeOffset();
        applyPID();
    }

    eBrake();
}

void applyPID() {
    double newSpeedLeft = speedLeft - speedOffset;
    newSpeedLeft = constrain(newSpeedLeft, speedDefault - 50, speedDefault + 50);
    double newSpeedRight = speedRight + speedOffset;
    newSpeedRight = constrain(newSpeedRight, speedDefault - 90, speedDefault + 10);
    motor.setSpeed(newSpeedLeft, newSpeedRight);
}

void eBrake() {
    movingLeft = false;
    movingRight = false;
    motor.brakeLeft();
    motor.brakeRight();
    moving = false;
    lastMoveTime = millis();
}

void resetMoveVars() {
    movingLeft = true;
    movingRight = true;
    sensor2Close = false;
    ticksLeft = 0;
    ticksRight = 0;
    speedOffset = 0;
    pid.reset();
    lps.reset();
    moving = true;
}

void align() {
    aligned = true;
    
    if (sensors.mayAlignLeft()) {
        alignLeft();
        return;
    }

    if (sensors.mayAlignFront()) alignFront();
}

void alignLeft() {
    double alignCounter = 0;
    
    while (alignCounter < 10) {
        double error = sensors.getErrorLeft();
        if (error >= 0.2 && error < 0.4) return;
        turn((error < 0.2) ? LEFT : RIGHT, 0.15);
        alignCounter++;
    }

    eBrake();
}

void alignFront() {
    double alignCounter = 0;
    
    while (alignCounter < 10) {
        double error = sensors.getErrorFront();
        if (error >= -0.3 && error < -0.1) return;
        turn((error < -0.3) ? RIGHT : LEFT, 0.15);
        alignCounter++;
    }

    eBrake();
    
}

void printSensorValues() {
    String s1 = String(sensors.getDistance(1));
    String s2 = String(sensors.getDistance(2));
    String s3 = String(sensors.getDistance(3));
    String s4 = String(sensors.getDistance(4));
    String s5 = String(sensors.getDistance(5));
    String s6 = String(sensors.getDistance(6));
    Serial.println("P" + s1 + "#" + s2 + "#" + s3 + "#" + s4 + "#" + s5 + "#" + s6);
    Serial.flush();
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
