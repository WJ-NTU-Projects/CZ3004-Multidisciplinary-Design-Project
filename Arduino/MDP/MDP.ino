#include <DualVNH5019MotorShield.h>
#include <EnableInterrupt.h>
#include <PID_v1.h>
#include "MDP.h"

DualVNH5019MotorShield md;
PID pidLeft(&rpmL, &outputSpeedL, &rpmTarget, kp, ki, kd, DIRECT);
PID pidRight(&rpmR, &outputSpeedR, &rpmTarget, kp, ki, kd, DIRECT);

void setup() {
    pinMode(M1E1Right, INPUT);
    pinMode(M2E2Left, INPUT);
    digitalWrite(M1E1Right, HIGH);       // turn on pull-up resistor
    digitalWrite(M2E2Left, HIGH);       // turn on pull-up resistor
    enableInterrupt(M1E1Right, E1, RISING);
    enableInterrupt(M2E2Left, E2, RISING);
    
    Serial.begin(115200);    
    md.init();
    pidLeft.SetOutputLimits(-MAX_SPEED, MAX_SPEED);   // change this value for PID calibration. This is the maximum speed PID sets to
    pidLeft.SetSampleTime(PID_SAMPLE_TIME);
    pidRight.SetOutputLimits(-MAX_SPEED, MAX_SPEED);   // change this value for PID calibration. This is the maximum speed PID sets to
    pidRight.SetSampleTime(PID_SAMPLE_TIME);

    for (int i = 0; i < 6; i++) {
        memset(sensorValues[i], 0, sizeof(sensorValues[i])); 
    }
    
    delay(3000);
    Serial.println("Start.");
    moveForward(15);
}

void loop() {
    // Hello World
}

void enablePID(boolean a) {
    pidLeft.SetMode(a? AUTOMATIC : MANUAL);
    pidRight.SetMode(a? AUTOMATIC : MANUAL);
}

void reset() {
    rpmTarget = 20;
    wavesL = wavesR = rpmL = rpmR = currentSpeedL = currentSpeedR = outputSpeedL = outputSpeedR = pulsePeriodL = pulsePeriodR = 0;
    moveEnabled = true;
    pulseTimeLastL = micros();
    pulseTimeLastR = micros();
}

void moveRobot(int directionR, int directionL) {
    reset();
    enablePID(true);
    moveLoop(directionR, directionL, false);
    wavesLimit = WAVES_BRAKE + 1;
    wavesL = 0;
    wavesR = 0;
    moveEnabled = true;
    
    Serial.println("Braking.");
    moveLoop(directionR, directionL, true);
    enablePID(false);
    md.setSpeeds(0, 0);
}

void moveLoop(int directionR, int directionL, boolean braking) {
    while (moveEnabled) {
        if (!braking) {
            if (!obstacleAhead) totalDistance = wavesL / WAVES_PER_ROTATION * DISTANCE_PER_ROTATION;
            if (directionR == directionL && directionR == 1) checkForObstacleAhead();
        }
        
        pidLeft.Compute();
        pidRight.Compute();
        currentSpeedL = max(outputSpeedL * 1.061, 0);
        currentSpeedR = max(outputSpeedR, 0);
        md.setSpeeds(directionR * currentSpeedR, directionL * currentSpeedL);
        rpmL = (pulsePeriodL == 0)? 0 : max(round(60000000.0 / (pulsePeriodL * WAVES_PER_ROTATION)), 0);
        rpmR = (pulsePeriodR == 0)? 0 : max(round(60000000.0 / (pulsePeriodR * WAVES_PER_ROTATION)), 0); 
    
        if (braking) {
            if (abs(rpmTarget - min(rpmL, rpmR)) < 10 && rpmTarget > 10) rpmTarget -= 10;
        } else {
            if (abs(rpmTarget - min(rpmL, rpmR)) <= 10 && rpmTarget < 60) rpmTarget += 10;
        }
        
        //Serial.println("Speed: " + String(currentSpeedL) + "/" + String(currentSpeedR) + ", RPM: " + String(rpmL) + "/" + String(rpmR) + ", RPM Target: " + String(rpmTarget));
    }
}

void evade(double grids, boolean diagonal) {
    int travelledGrids = ((totalDistance + 2) / 10) + 1;     
    if (grids == travelledGrids) return;
    
    avoiding = true;
    obstacleAhead = false;
    boolean left = (getIRDistance(sensor4, A3m, A3c) >= 20 && getIRDistance(sensor5, A4m, A4c) >= 20); 

    if (diagonal) {
        if (left) turnLeft(45); else turnRight(45);
        delay(100);
        moveForward(sqrt(18));
        delay(100);
        if (left) turnRight(90); else turnLeft(90);
        delay(100);
        moveForward(sqrt(18));
        delay(100);
        if (left) turnLeft(45); else turnRight(45);
        delay(100);
        moveForward(grids - travelledGrids - 6);
    } else {
        if (left) turnLeft(90); else turnRight(90);
        delay(100);
        moveForward(2);
        delay(100);
        if (left) turnRight(90); else turnLeft(90);
        delay(100);
        moveForward(4);
        delay(100);
        if (left) turnRight(90); else turnLeft(90);
        delay(100);
        moveForward(2);
        delay(100);
        if (left) turnLeft(90); else turnRight(90);
        delay(100);
        moveForward(grids - travelledGrids - 4);  
    }
    
    avoiding = false;
}

void doMove(double grids, boolean forward) {
    wavesLimit = round(WAVES_PER_GRID * grids - WAVES_BRAKE);
    if (forward) moveRobot(1, 1); else moveRobot(-1, -1);
}

void doTurn(int angle, boolean left) {
    wavesLimit = round((ON_PAPER? WAVES_PER_ANGLE_COMPASS : WAVES_PER_ANGLE_ARENA) * angle - WAVES_BRAKE);
    if (left) moveRobot(1, -1); else moveRobot(-1, 1);
}

void moveForward(double grids) {
    Serial.println("Moving forward.");
    doMove(grids, true);
    if (obstacleAhead) evade(grids, DIAGONAL_EVADE);
}

void moveReverse(int grids) {
    Serial.println("Reversing.");
    doMove(grids, false);
}

void turnLeft(int angle) {
    Serial.println("Turning Left.");
    doTurn(angle, true);
}

void turnRight(int angle) {
    Serial.println("Turning Right.");
    doTurn(angle, false);
}

void checkForObstacleAhead() {
    if (DIAGONAL_EVADE) {
        if (getIRDistance(sensor2, A1m, A1c) <= 26 && !obstacleAhead && !avoiding) {
            Serial.println("Detected obstacle ahead.");
            obstacleAhead = true;
            wavesL = 0;
            wavesR = 0;
            wavesLimit = WAVES_BRAKE;
        }

        return;
    }

    if (getIRDistance(sensor2, A1m, A1c) == 10 && !obstacleAhead && !avoiding) {
        Serial.println("Detected obstacle ahead.");
        obstacleAhead = true;
        wavesL = 0;
        wavesR = 0;
        wavesLimit = WAVES_BRAKE;
    }
}

int getIRDistance(char sensor, double m, double c) {
    int index = -1;
    
    switch (sensor) {
        case sensor1: index = 0; break;
        case sensor2: index = 1; break;
        case sensor3: index = 2; break;
        case sensor4: index = 3; break;
        case sensor5: index = 4; break;
        case sensor6: index = 5; break;
    }

    if (index == -1) return -1;
    int raw = analogRead(sensor);
    sensorValues[index][sensorCounter[index]] = raw;
    sensorCounter[index]++;
    if (sensorCounter[index] >= SENSOR_ROLLING_AVERAGE_COUNT) sensorCounter[index] = 0;

    int average = 0;
    int count = 0;
    
    for (int i = 0; i < SENSOR_ROLLING_AVERAGE_COUNT; i++) {
        int value = sensorValues[index][i];
        
        if (value > 0) {
            average += value;
            count++;
        }
    }

    average = (average / count);
    double volts = map(raw, 0, 1023, 0, 5000) / 1000.0;
    int dist = round((1 / (volts * m + c)) - ((index < 5)? 1.32 : 1.02));

    if (index == 1) {
        int t10 = sensorRaw[index];
        if (dist <= 20 && average >= t10) dist = 10;
        if (sensorDistance[index] == 10 && dist <= 20) return 10;
    }
    
    if ((dist > 25) && (index < 5 || (index == 5 && dist <= 48))) dist += 1.5; 
    dist = abs(dist);
    sensorDistance[index] = dist;
    Serial.println("Average Reading = " + String(average));
    return dist;
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

        // HACK
        counter++;
        if (counter >= 2000) break;
    } 

    //Serial.println("INPUT = " + inputString);
 
    if (inputString.indexOf("s") == 0) {
        String distanceStr = inputString.substring(1);
        
        if (isNumber(distanceStr)) {
            int distance = distanceStr.toInt();
            moveForward(distance);
        }  
    } else if (inputString.indexOf("r") == 0) {        
        String distanceStr = inputString.substring(1);
        
        if (isNumber(distanceStr)) {
            int distance = distanceStr.toInt();
            moveReverse(distance);
        }   
    } else if (inputString.indexOf("tl") == 0) {
        String angleStr = inputString.substring(2);
        
        if (isNumber(angleStr)) {
            int angle = angleStr.toInt();
            turnLeft(angle);
        }
    } else if (inputString.indexOf("tr") == 0) {
        String angleStr = inputString.substring(2);
        
        if (isNumber(angleStr)) {
            int angle = angleStr.toInt();
            turnRight(angle);
        } 
    } else if (inputString.indexOf("reset") == 0) {
        obstacleAhead = false;
    }
}

void E1() {
    pulseTimeNowL = micros();
    pulsePeriodL = (pulseTimeNowL - pulseTimeLastL);
    pulseTimeLastL = pulseTimeNowL;
    wavesL++;
    if (wavesL >= wavesLimit) moveEnabled = false;
}

void E2() {
    pulseTimeNowR = micros();
    pulsePeriodR = (pulseTimeNowR - pulseTimeLastR);
    pulseTimeLastR = pulseTimeNowR;
    wavesR++;
    if (wavesR >= wavesLimit) moveEnabled = false;
}

boolean isNumber(String str) {
    for (byte i = 0; i < str.length(); i++) {
        if (!isDigit(str.charAt(i))) return false;
    }

    return true;
}
