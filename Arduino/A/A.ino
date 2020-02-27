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
    align();
    delay(100);
    move(FORWARD, 2000);
    align();
    delay(100);
    turn(LEFT, 90);
    align();
    delay(100);
    move(FORWARD, 2000);
    align();
    delay(100);
    turn(RIGHT, 90);
    align();
    delay(100);
    turn(RIGHT, 90);
    align();
    delay(100);
    move(FORWARD, 2000);
    align();
    delay(100);
    turn(RIGHT, 90);
    align();
    delay(100);
    move(FORWARD, 2000);
    align();
}

void loop() {
    double distance1 = sensors.getDistance(1);
    double distance2 = sensors.getDistance(3);
    double d = (distance1 + distance2) * 0.5;
    Serial.println(sensors.getErrorLeft());
    delay(5);
    return;

//    if (!moving) {
//        debugRun();
//        return;
//    }

    while (Serial.available() > 0) {
        char input = (char) Serial.read();
        Serial.println("D" + String(input));
        
        if (input == '\n') break;
        
        if (input == 'I') {
            printSensorValues();
            break;
        }
        
        if (input == 'M') move(FORWARD, 100);
        else if (input == 'L') turn(LEFT, 90);
        else if (input == 'R') turn(RIGHT, 90);
        else if (input == 'C') align();
        else return;

//        delay(5);
//        Serial.print("P" + String(input));
//        Serial.flush();
    }
}

void debugRun() {    
//    if (!sensors.hasObstacleLeft(12)) {
//        delay(100);
//        turn(LEFT, 90);
//        delay(50);
//        align();
//        delay(50);
//    } else if (sensors.hasObstacleFront(8)) {
//        delay(100);
//        turn(RIGHT, 90);
//        delay(50);
//        align();
//        delay(50);
//        
//        if (sensors.hasObstacleFront(6)) {
//            delay(100);
//            turn(RIGHT, 90);
//            delay(50);
//            align();
//            delay(50);
//        }
//    }
//
//    if (!sensors.hasObstacleFront(6)) {
//        move(FORWARD, 1200);
//        align();
//    }
}

void move(int direction, int distance) {
    if (direction != FORWARD && direction != REVERSE) return;
    ticksTarget = distance * TICKS_PER_MM;
    localX = 0;
    localY = 0;
    speedLeft = speedDefault;
    speedRight = speedDefault - 30;
    resetMoveVars();

    if (direction == FORWARD)  motor.forward(speedLeft, speedRight);
    else motor.reverse(speedLeft, speedRight);

    while (movingLeft || movingRight) {           
//        if (sensors.getDistanceR(2) <= 15 && !sensor2Close) sensor2Close = true;   
//             
//        if (sensors.hasObstacleFront(6)) {
//            break;
//        }

//        double setpointOffset = 0.01;
//
//        if (sensors.mayAlignLeft()) {
//            double error = sensors.getErrorLeft();
//            if (error < -0.2) setpoint = setpointOffset;
//            else if (error > 0.4) setpoint = -setpointOffset;
//            else setpoint = 0; 
//        } else {
//            if (sensors.mayAlignFront()) {
//                double error = sensors.getErrorFront();                
//                if (error < -0.3) setpoint = -setpointOffset;
//                else if (error > 0.3) setpoint = setpointOffset;
//                else setpoint = 0; 
//            } else {
//                setpoint = 0;
//            }
//        }
        
        lps.computePosition();
        localX = lps.getX();

        if (localX >= distance) {
            break;
        }
        
        localY = lps.getY();
        speedOffset = pid.computeOffset();
        applyPID();

//        int mod = round(localX) % 100;
//        
//        if (localX >= 100 && mod >= 0 && mod <= 10 && !sensors.hasObstacleLeft(12)) {
//            eBrake();
//            return;    
//        }

        Serial.println(localY);
        delay(10);
    }

    eBrake();
}

void turn(int direction, double angle) {
    if (direction != LEFT && direction != RIGHT) return;
    ticksTarget = angle * TICKS_PER_ANGLE;
    double distance = ticksTarget / TICKS_PER_MM;
    speedLeft = EXPLORE_SPEED_LEFT;
    speedRight = speedLeft - 30;
    resetMoveVars();
    aligned = false;
    
    if (direction == RIGHT) motor.turnRight(speedLeft, speedRight);
    else motor.turnLeft(speedLeft, speedRight);

    while (movingLeft || movingRight) {
        lps.computePosition();
        localX = lps.getX();

        if (localX >= distance) {
            break;
        }
        
        localY = lps.getY();
        speedOffset = pid.computeOffset();
        applyPID();
        
        delay(5);        
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
    if (sensors.mayAlignLeft()) {
        alignLeft();        
        aligned = true;
        return;
    }

    if (sensors.mayAlignFront()) {
        alignFront();
        aligned = true;
    }
}

void alignLeft() {
    double alignCounter = 0;
    
    while (alignCounter < 5) {
        double error = sensors.getErrorLeft();
        if (error >= -0.25 && error <= 0.25) return;
        turn((error < -0.25) ? LEFT : RIGHT, 0.2);
        alignCounter++;
        delay(5);
    }

    eBrake();
}

void alignFront() {
    double alignCounter = 0;
    
    while (alignCounter < 5) {
        double error = sensors.getErrorFront();
        if (error >= -0.25 && error <= 0.25) return;
        turn((error < -0.25) ? RIGHT : LEFT, 0.2);
        alignCounter++;
    }

    return;
    double distance1 = sensors.getDistance(1);
    double distance2 = sensors.getDistance(3);
    double averageDistance = (distance1 + distance2) * 0.5;

    if (distance1 < 0 || distance1 > 9) {
        return;
    }

    if (distance2 < 0 || distance2 > 9) {
        return;
    }
       
    while (averageDistance > 4.5 && averageDistance <= 9) {
        move(FORWARD, 10);
        distance1 = sensors.getDistance(1);
        distance2 = sensors.getDistance(3);
        averageDistance = (distance1 + distance2) * 0.5;
        delay(5);
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
//    Serial.println("PI");
//    Serial.flush();
    Serial.println("P" + s1 + "#" + s2 + "#" + s3 + "#" + s4 + "#" + s5 + "#" + s6);
    Serial.flush();
}

void interruptLeft() {
    if (!movingLeft) return;
    ticksLeft += 0.5;
    
//    if (abs(ticksLeft - ticksTarget) <= 0.25) {
//        motor.brakeLeft();
//        movingLeft = false;
//    }
}

void interruptRight() {
    if (!movingRight) return;
    ticksRight += 0.5;

//    if (abs(ticksRight - ticksTarget) <= 0.25) {
//        motor.brakeRight();
//        movingRight = false;
//    }
}
