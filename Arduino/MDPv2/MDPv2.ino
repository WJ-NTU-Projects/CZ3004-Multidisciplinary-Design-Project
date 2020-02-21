#include <EnableInterrupt.h>
#include <PID_v1.h>
#include "MDPv2.h"
#include "Motor.h"

Motor motor;
PID pidLeft(&rpmL, &outputSpeedL, &rpmTarget, 2, 40, kd, DIRECT);
PID pidRight(&rpmR, &outputSpeedR, &rpmTarget, 1.25, 32, kd, DIRECT);
int loopCounter = 1;
boolean stopp = false;
boolean dir = forward;
void setup() {
    pinMode(M1E1Right, INPUT);
    pinMode(M2E2Left, INPUT);
    digitalWrite(M1E1Right, HIGH);       
    digitalWrite(M2E2Left, HIGH);       
    enableInterrupt(M1E1Right, E1, RISING);
    enableInterrupt(M2E2Left, E2, RISING);

    Serial.begin(115200);
    pidLeft.SetOutputLimits(-MAX_SPEED, MAX_SPEED);  
    pidLeft.SetSampleTime(PID_SAMPLE_TIME);
    pidRight.SetOutputLimits(-MAX_SPEED, MAX_SPEED);   
    pidRight.SetSampleTime(PID_SAMPLE_TIME);
    enablePID(false);

    delay(3000);
    //turnRobot(right, 900);
    moveRobot(forward, 15);
    //if (obstacleAhead) evade(15, true);
//    return;
    
//    delay(500);
//    turnRobot(right, 90);
//    delay(500);
//    moveRobot(forward, 10);
//    delay(500);
//    turnRobot(right, 90);
//    delay(500);
//    moveRobot(forward, 10);
//    delay(500);
//    turnRobot(right, 90);
//    delay(500);
//    moveRobot(forward, 10);
//    return;
//    turnRobot(right, 90);
//    delay(500);
//    turnRobot(right, 90);
//    delay(500);
//    turnRobot(right, 90);
//    return;
//    delay(500);
//    turnRobot(left, 90);
//    delay(500);
//    turnRobot(left, 90);
//    delay(500);
//    turnRobot(left, 90);
//    delay(500);
//    turnRobot(left, 90);
//    delay(500);
//    turnRobot(right, 850);
//    delay(500);
//    turnRobot(right, 230);
//    delay(500);
//    turnRobot(left, 850);
//    delay(500);
//    turnRobot(left, 230);
}

void loop() {
//    Serial.println(IRSensor(sensor1, A0m, A0c));
//    Serial.println(IRSensor(sensor3, A2m, A2c));
//    Serial.println();
//    delay(1000);
//    alignFrontToWall();
}

void alignFrontToWall() {
    float readingSensorA = IRSensor(sensor1, A0m, A0c);
    float readingSensorB = IRSensor(sensor3, A2m, A2c);
    if (readingSensorA < 0 || readingSensorB < 0) return;
    
    float average = (readingSensorA + readingSensorB) / 2;
    if (average <= 10) motor.brake(); 
    else {
        dir = forward;
        motor.moveForward(100, 130);
        return;
    }
    
    float error = readingSensorA - readingSensorB;
    
    while (average > 0 && average <= 10 && abs(error) > 0.5) {
        // adjustment to ensure front of robot is parallel to wall
        if (error > 0.5) {
          turnRobot(right, 1);
          error = IRSensor(sensor1, A0m, A0c) - IRSensor(sensor3, A2m, A2c);
        } else if (error < -0.5) {
          turnRobot(left, 1);
          error = IRSensor(sensor1, A0m, A0c) - IRSensor(sensor3, A2m, A2c);
        }
    }
  
    int count = 0;
    // adjustment to ensure front of robot is of an acceptable range between front and wall
    float avgReading = (IRSensor(sensor1, A0m, A0c) + IRSensor(sensor3, A2m, A2c)) / 2;

    if (avgReading > 0 && avgReading <= 10) {
        while (count < 10) {
            int difference = avgReading - 6;
    
            if (dir == forward) {
                obstacleAhead = false;
                
                if (difference < 0) {
                    moveRobot(reverse, abs(difference * 0.1));
                    avgReading = (IRSensor(sensor1, A0m, A0c) + IRSensor(sensor3, A2m, A2c)) / 2;
                    count++;
                } else if (difference > 0) {
                    moveRobot(forward, abs(difference * 0.1));
                    avgReading = (IRSensor(sensor1, A0m, A0c) + IRSensor(sensor3, A2m, A2c)) / 2;
                    count++;
                }
                
                Serial.println(difference);
            }
        }

        turnRobot(right, 90);

        readingSensorA = IRSensor(sensor4, A3m, A0c);
        readingSensorB = IRSensor(sensor5, A4m, A2c);
        error = readingSensorA - readingSensorB;
    
        while (abs(error) > 0.5) {
            // adjustment to ensure front of robot is parallel to wall
            if (error > 0.5) {
              turnRobot(right, 1);
              error = IRSensor(sensor4, A3m, A3c) - IRSensor(sensor5, A4m, A4c);
            } else if (error < -0.5) {
              turnRobot(left, 1);
              error = IRSensor(sensor4, A3m, A3c) - IRSensor(sensor5, A4m, A4c);
            }
        }
    }

}

void enablePID(boolean a) {
    pidLeft.SetMode(a? AUTOMATIC : MANUAL);
    pidRight.SetMode(a? AUTOMATIC : MANUAL);
}

void reset() {
    wavesL = wavesR = rpmL = rpmR = currentSpeedL = currentSpeedR = outputSpeedL = outputSpeedR = pulsePeriodL = pulsePeriodR = 0;
    moveEnabled = true;
    pulseTimeLastL = micros();
    pulseTimeLastR = micros();
}

void moveRobot(Direction direction, double distance) {
    if (obstacleAhead) return;
    if (direction == left || direction == right) return;
    reset();
    rpmTarget = 30;
    enablePID(true);
    wavesLimit = round(WAVES_PER_GRID * distance);

    while (moveEnabled) {
        //if (!obstacleAhead) totalDistance = wavesL / WAVES_PER_ROTATION * DISTANCE_PER_ROTATION;
        //if (direction == forward) checkForObstacleAhead();
        pidLeft.Compute();
        pidRight.Compute();
        currentSpeedL = max(outputSpeedL, 0); //150cm 1.061 
        currentSpeedR = max(outputSpeedR, 0); 
        if (direction == forward) motor.moveForward(currentSpeedR, currentSpeedL);
        else if (direction == reverse) motor.moveReverse(currentSpeedR, currentSpeedL);
        rpmL = (pulsePeriodL == 0)? 0 : max(round(60000000.0 / (pulsePeriodL * WAVES_PER_ROTATION)), 0);
        rpmR = (pulsePeriodR == 0)? 0 : max(round(60000000.0 / (pulsePeriodR * WAVES_PER_ROTATION)), 0); 
        
        if (wavesL >= wavesLimit - (WAVES_PER_GRID * (MAX_RPM * 0.01)) || wavesR >= wavesLimit - (WAVES_PER_GRID * (MAX_RPM * 0.01))) {
            if (rpmTarget > 20 && round(rpmL) <= rpmTarget && round(rpmR) <= rpmTarget) {
                rpmTarget -= 10;
            }
        } else {
            if (rpmTarget < MAX_RPM && round(rpmL) >= rpmTarget && round(rpmR) >= rpmTarget) {
                rpmTarget += 10;
            }
        }
        
        //Serial.println("Speed: " + String(currentSpeedL) + " / " + String(currentSpeedR) + ", RPM: " + String(round(rpmL)) + " / " + String(round(rpmR)) + ", RPM Target: " + String(rpmTarget));
    }
    
    motor.brake();
    enablePID(false);
    reset();
}

void turnRobot(Direction direction, double angle) {
    if (direction == forward || direction == reverse) return;
    reset();
    rpmTarget = 40;
    enablePID(true);
    wavesLimit = round((direction == right? WAVES_PER_ANGLE_COMPASS : WAVES_PER_ANGLE_COMPASS) * angle);

    while (moveEnabled) {
        pidLeft.Compute();
        pidRight.Compute();
        currentSpeedL = max(outputSpeedL, 0); //150cm 1.061 
        currentSpeedR = max(outputSpeedR, 0); 
        if (direction == right) motor.turnRight(currentSpeedR, currentSpeedL);
        else if (direction == left) motor.turnLeft(currentSpeedR, currentSpeedL);
        rpmL = (pulsePeriodL == 0)? 0 : max(60000000.0 / (pulsePeriodL * WAVES_PER_ROTATION), 0);
        rpmR = (pulsePeriodR == 0)? 0 : max(60000000.0 / (pulsePeriodR * WAVES_PER_ROTATION), 0); 
        //Serial.println("Speed: " + String(currentSpeedL) + " / " + String(currentSpeedR) + ", RPM: " + String(round(rpmL)) + " / " + String(round(rpmR)) + ", RPM Target: " + String(rpmTarget));
    }
    
    motor.brake();
    enablePID(false);
    reset();
}

void checkForObstacleAhead() {
    if (IRSensor2(sensor2, A1m, A1c) <= 20 && !obstacleAhead && !avoiding) {
        Serial.println("Detected obstacle ahead.");
        obstacleAhead = true;
        wavesL = 0;
        wavesR = 0;
        wavesLimit = round(WAVES_PER_GRID * 0.75);
    }
}

void evade(double grids, boolean diagonal) {
    int travelledGrids = ((totalDistance + 2) / 10) + 1 ;     
    if (grids == travelledGrids) return;
    
    avoiding = true;
    obstacleAhead = false;
    boolean l = (IRSensor(sensor4, A3m, A3c) >= 30 && IRSensor(sensor5, A4m, A4c) >= 30); 

    if (l) turnRobot(left, 45); else turnRobot(right, 45);
    delay(100);
    moveRobot(forward, sqrt(18));
    delay(100);
    if (l) turnRobot(right, 90); else turnRobot(left, 90);
    delay(100);
    moveRobot(forward, sqrt(18));
    delay(100);
    if (l) turnRobot(left, 45); else turnRobot(right, 45);
    delay(100);
    moveRobot(forward, grids - travelledGrids - 6);
    
    avoiding = false;
}

float IRSensor(char sensor, float m, float c) {
   // for sensors 1-5
  int raw=analogRead(sensor);
  int voltFromRaw=map(raw, 0, 1023, 0, 5000);
    
  float volts = voltFromRaw/1000.0;
  float distance = (1/((volts)*m + c))-0.42;
  delay(10);                    // decrease value for faster readings 
  //return distance;
  if (distance<12) {
    return distance - 0.5;
  }
  else if (distance> 20) {
    return distance+1;
  }
  else if(distance>17.5){
    return distance+0.5;
  }
  else{
    return distance;
  }
}

float IRSensor2(char sensor, float m, float c) {
  //only for sensor 6, eff range 22-60cm, >60cm range deviation too large
  int raw=analogRead(sensor);
  int voltFromRaw=map(raw, 0, 1023, 0, 5000);
    
  float volts = voltFromRaw/1000.0;
  float distance = (1/(volts*m + c))-1.52;          
  delay(10);                    // decrease value for faster readings 
  if (distance>30){
    return distance+1;
  }
  else{
    return distance;
  }
}

float IRSensor6(char sensor, float m, float c) {
  //only for sensor 6, eff range 22-60cm, >60cm range deviation too large
  int raw=analogRead(sensor);
  int voltFromRaw=map(raw, 0, 1023, 0, 5000);
    
  float volts = voltFromRaw/1000.0;
  float distance = (1/(volts*m + c))-1.52;          
  delay(10);                    // decrease value for faster readings 
  if (distance<25){
    return distance-1;
  }
  else{
    return distance;
  }
//   return distance;
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
    //Serial.println("Average Reading = " + String(average));
    return dist;
}

void E1() {
    pulseTimeNowL = micros();
    pulsePeriodL = (pulseTimeNowL - pulseTimeLastL);
    pulseTimeLastL = pulseTimeNowL;
    wavesL++;
    
    if (wavesL == wavesLimit) {
        moveEnabled = false;
        motor.brake();
    }
}

void E2() {
    pulseTimeNowR = micros();
    pulsePeriodR = (pulseTimeNowR - pulseTimeLastR);
    pulseTimeLastR = pulseTimeNowR;
    wavesR++;
    
    if (wavesR == wavesLimit) {
        moveEnabled = false;
        motor.brake();
    }
}



boolean isNumber(String str) {
    for (byte i = 0; i < str.length(); i++) {
        if (!isDigit(str.charAt(i))) return false;
    }

    return true;
}
