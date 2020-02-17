#include <DualVNH5019MotorShield.h>
#include <EnableInterrupt.h>
#include <PID_v1.h>
#include <QuickMedianLib.h>

#define M1E1Right 11
#define M2E2Left 3
#define sensor1 A0
#define sensor2 A1
#define sensor3 A2
#define sensor4 A3
#define sensor5 A4
#define sensor6 A5
#define A0m 0.0444
#define A0c -0.0081
#define A1m 0.0171
#define A1c -0.0028
#define A2m 0.0433
#define A2c -0.0028
#define A3m 0.0374
#define A3c -0.003
#define A4m 0.0382
#define A4c -0.0018
#define A5m 0.0185
#define A5c -0.0034

volatile int wavesThreshold = 0;
volatile int wavesLeft = 0;
volatile int wavesRight = 0;
volatile long currentPulseLeft = 0;
volatile long currentPulseRight = 0;
volatile long pulseTimeLeft = 0;
volatile long previousPulseLeft = 0;
volatile long previousPulseRight = 0;
volatile long pulseTimeRight = 0;
volatile boolean enabled = false;
volatile boolean obstacleAhead = false;

double kp = 1.8, ki = 35, kd = 0;
double speedLeft = 0, speedRight = 0;
double speedOutputLeft = 0, speedOutputRight = 0;
double rpmLeft = 0, rpmRight = 0, rpmTarget = 0;

int sensorCounter[6] = {0, 0, 0, 0, 0, 0};
int sensorValues[6][25];
int sensorRaw[2] = {0, 580};
int sensorDistance[6] = {999, 999, 999, 999, 999, 999};

DualVNH5019MotorShield md;
PID pidLeft(&rpmLeft, &speedOutputLeft, &rpmTarget, kp, ki, kd, DIRECT);
PID pidRight(&rpmRight, &speedOutputRight, &rpmTarget, kp, ki, kd, DIRECT);

void setup() {
    pinMode(M1E1Right, INPUT);
    pinMode(M2E2Left, INPUT);
    digitalWrite(M1E1Right, HIGH);       // turn on pull-up resistor
    digitalWrite(M2E2Left, HIGH);       // turn on pull-up resistor
    enableInterrupt(M1E1Right, E1, RISING);
    enableInterrupt(M2E2Left, E2, RISING);
    
    Serial.begin(115200);    
    md.init();
    pidLeft.SetOutputLimits(-250, 250);   // change this value for PID calibration. This is the maximum speed PID sets to
    pidLeft.SetSampleTime(5);
    pidRight.SetOutputLimits(-250, 250);   // change this value for PID calibration. This is the maximum speed PID sets to
    pidRight.SetSampleTime(5);

    for (int i = 0; i < 6; i++) {
        memset(sensorValues[i], 0, sizeof(sensorValues[i])); 
    }
    
    delay(2000);
    moveForward(1);
    delay(1000);
    moveReverse(1);
    delay(1000);
    moveForward(2);
    delay(1000);
    moveReverse(2);
    delay(1000);
    moveForward(3);
    delay(1000);
    moveReverse(3);
    delay(1000);
    moveForward(4);
    delay(1000);
    moveReverse(4);
}

void loop() {}

void setupPID(boolean a) {
    pidLeft.SetMode(a? AUTOMATIC : MANUAL);
    pidRight.SetMode(a? AUTOMATIC : MANUAL);
}

void reset() {
    rpmTarget = 20;
    wavesLeft = wavesRight = rpmLeft = rpmRight = speedLeft = speedRight = speedOutputLeft = speedOutputRight = pulseTimeLeft = pulseTimeRight = 0;
    enabled = true;
    previousPulseLeft = micros();
    previousPulseRight = micros();
}

void moveRobot(int directionRight, int directionLeft) {
    reset();
    setupPID(true);

    while (enabled) {
        if (directionRight == directionLeft) checkForObstacleAhead();
        pidLeft.Compute();
        pidRight.Compute();
        speedLeft = max(speedOutputLeft * 1.061, 0);
        speedRight = max(speedOutputRight, 0);
        md.setSpeeds(directionRight * speedRight, directionLeft * speedLeft);
        rpmLeft = (pulseTimeLeft == 0)? 0 : max(round(60000000.0 / (pulseTimeLeft * 562.25)), 0);
        rpmRight = (pulseTimeRight == 0)? 0 : max(round(60000000.0 / (pulseTimeRight * 562.25)), 0); 
        if (abs(rpmTarget - min(rpmLeft, rpmRight)) <= 10 && rpmTarget < 60) rpmTarget += 10;
        Serial.println("Speed: " + String(speedLeft) + "/" + String(speedRight) + ", RPM: " + String(rpmLeft) + "/" + String(rpmRight) + ", RPM Target: " + String(rpmTarget));
    }

    wavesThreshold = 150;
    wavesLeft = 0;
    wavesRight = 0;
    enabled = true;

    while (enabled) {
        pidLeft.Compute();
        pidRight.Compute();
        speedLeft = max(speedOutputLeft * 1.061, 0);
        speedRight = max(speedOutputRight, 0);
        md.setSpeeds(directionRight * speedRight, directionLeft * speedLeft);
        rpmLeft = (pulseTimeLeft == 0)? 0 : max(round(60000000.0 / (pulseTimeLeft * 562.25)), 0);
        rpmRight = (pulseTimeRight == 0)? 0 : max(round(60000000.0 / (pulseTimeRight * 562.25)), 0); 
        if (abs(rpmTarget - min(rpmLeft, rpmRight)) < 10 && rpmTarget > 10) rpmTarget -= 10;
        Serial.println("Speed: " + String(speedLeft) + "/" + String(speedRight) + ", RPM: " + String(rpmLeft) + "/" + String(rpmRight) + ", RPM Target: " + String(rpmTarget));
    }
    
    setupPID(false);
    md.setSpeeds(0, 0);
}

void moveForward(int moveDistance) {
    wavesThreshold = round(298.28289 * moveDistance - 149);
    moveRobot(1, 1);
    if (obstacleAhead) turnRight(90);
}

void moveReverse(int moveDistance) {
    wavesThreshold = round(298.28289 * moveDistance - 149);
    moveRobot(-1, -1);
}

void turnLeft(int angle) {
    wavesThreshold = round(4.72 * angle) - 71;
    moveRobot(1, -1);
}

void turnRight(int angle) {
    wavesThreshold = round(4.72 * angle) - 71;
    moveRobot(-1, 1);
}

void checkForObstacleAhead() {
    if (getIRDistance(sensor2, A1m, A1c) == 10 && !obstacleAhead) {
        obstacleAhead = true;
        wavesLeft = 0;
        wavesRight = 0;
        wavesThreshold = 149;
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
    if (sensorCounter[index] >= 25) sensorCounter[index] = 0;

    int average = 0;
    int count = 0;
    
    for (int i = 0; i < 25; i++) {
        int value = sensorValues[index][i];
        
        if (value > 0) {
            average += value;
            count++;
        }
    }

    average = (average / count);
    double volts = map(raw, 0, 1023, 0, 5000) / 1000.0;
    int dist = round((1 / (volts * m + c)) - 1.32);
    int t10 = sensorRaw[index];
    if (dist <= 20 && average >= t10) dist = 10;
    if (sensorDistance[index] == 10 && dist <= 20) return 10;
    sensorDistance[index] = dist;
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

        counter++;
        if (counter >= 2000) break;
    } 

    Serial.println("INPUT = " + inputString);
 
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
    currentPulseLeft = micros();
    pulseTimeLeft = (currentPulseLeft - previousPulseLeft);
    previousPulseLeft = currentPulseLeft;
    wavesLeft++;
    if (wavesLeft >= wavesThreshold) enabled = false;
}

void E2() {
    currentPulseRight = micros();
    pulseTimeRight = (currentPulseRight - previousPulseRight);
    previousPulseRight = currentPulseRight;
    wavesRight++;
    if (wavesRight >= wavesThreshold) enabled = false;
}

boolean isNumber(String str) {
    for (byte i = 0; i < str.length(); i++) {
        if (!isDigit(str.charAt(i))) return false;
    }

    return true;
}
