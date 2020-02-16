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

DualVNH5019MotorShield md;

int wavesThreshold = 0;
int wavesLeft = 0;
int wavesRight = 0;
volatile long currentPulseLeft = 0;
volatile long currentPulseRight = 0;
volatile long pulseTimeLeft = 0;
volatile long previousPulseLeft = 0;
volatile long previousPulseRight = 0;
volatile long pulseTimeRight = 0;
volatile boolean enabled = false;
volatile boolean obstacleAhead = false;

double currentL = 0;
double currentR = 0;

//PID
double rpml = 0;
double rpmr = 0;
double targetRpm = 15;
double speedOffsetLeft = 0;
double speedOffsetRight = 0;
double maxRpmLeft = 0;
double maxRpmRight = 0;

// Sensor
int sensorCounter[6] = {0, 0, 0, 0, 0, 0};
int sensorValues[6][25];

int sensorRaw[2] = {0, 580};

int sensorDistance[6] = {999, 999, 999, 999, 999, 999};

PID leftPID(&rpml, &speedOffsetLeft, &targetRpm, 1.8, 35, 0, REVERSE);
PID rightPID(&rpmr, &speedOffsetRight, &targetRpm, 1.8, 35, 0, REVERSE);

void setupPID(boolean a) {
    if (a) {
        leftPID.SetMode(AUTOMATIC);
        rightPID.SetMode(AUTOMATIC);
    } else {
        leftPID.SetMode(MANUAL);
        rightPID.SetMode(MANUAL);
    }
}

void setup() {
    pinMode(M1E1Right, INPUT);
    pinMode(M2E2Left, INPUT);
    digitalWrite(M1E1Right, HIGH);       // turn on pull-up resistor
    digitalWrite(M2E2Left, HIGH);       // turn on pull-up resistor
    enableInterrupt(M1E1Right, E1, RISING);
    enableInterrupt(M2E2Left, E2, RISING);
    Serial.begin(115200);
    Serial.println("Dual VNH5019 Motor Shield");
    
    md.init();
    leftPID.SetOutputLimits(-50, 50);   // change this value for PID calibration. This is the maximum speed PID sets to
    leftPID.SetSampleTime(5);
    rightPID.SetOutputLimits(-50, 50);   // change this value for PID calibration. This is the maximum speed PID sets to
    rightPID.SetSampleTime(5);

    for (int i = 0; i < 6; i++) {
        memset(sensorValues[i], 0, sizeof(sensorValues[i])); 
    }
    
    Serial.println("start");  
    delay(2000);
        
    moveRobot(1, 4, 60);
    delay(500);
    moveRobot(-1, 1, 60);
    delay(500);
    moveRobot(-1, 1, 60);
    delay(500);
    moveRobot(-1, 1, 60);
    delay(500);
    moveRobot(-1, 1, 60);
    delay(500);
    moveRobot(1, 3, 60);
    delay(500);
    moveRobot(-1, 1, 60);
    delay(500);
    moveRobot(-1, 1, 60);
    delay(500);
    moveRobot(-1, 1, 60);
    delay(500);
    moveRobot(1, 2, 60);
    delay(500);
    moveRobot(-1, 1, 60);
    delay(500);
    moveRobot(-1, 1, 60);
    delay(500);
    moveRobot(1, 1, 60);
    delay(500);
    moveRobot(-1, 1, 60);
    delay(500);
    moveRobot(1, 2, 60);
}

void loop() {}

void reset() {
    wavesLeft = 0;
    wavesRight = 0; 
    rpml = 0;
    rpmr = 0;
    targetRpm = 10;
    currentL = 1;
    currentR = 0;
    speedOffsetLeft = 0;
    speedOffsetRight = 0;
    pulseTimeLeft = 0;
    pulseTimeRight = 0;
    enabled = true;
}

void results(int dir, int dist, int offset) {
    if (obstacleAhead) return;
    wavesThreshold = 298 * dist - 81;
    reset();
    setupPID(true);
    
    while(enabled) {
        if (getIRDistance(sensor2, A1m, A1c) == 10 && !obstacleAhead) {
            obstacleAhead = true;
            wavesLeft = 0;
            wavesRight = 0;
            wavesThreshold = 149;
        }
        
        leftPID.Compute();
        rightPID.Compute();
        currentL -= (speedOffsetLeft* 1.05);
        currentL = min(currentL, 350);
        currentR -= (speedOffsetRight); 
        currentR = min(currentR, 350);
        md.setSpeeds(dir * currentR, dir * currentL);   
        delay(25);
        
        if (getIRDistance(sensor2, A1m, A1c) == 10 && !obstacleAhead) {
            obstacleAhead = true;
            wavesLeft = 0;
            wavesRight = 0;
            wavesThreshold = 149;
        }
         
        rpml = max(round(60000000.0 / (pulseTimeLeft * 562.25)), 0);
        rpmr = max(round(60000000.0 / (pulseTimeRight * 562.25)), 0); 
        if (pulseTimeLeft == 0) rpml = 0;
        if (pulseTimeRight == 0) rpmr = 0;
        if (targetRpm < 60) targetRpm += 10;
    }

    
    brake(dir, dir);
    if (obstacleAhead) turn(false, 90);
}

void brake(int dirR, int dirL) {
    targetRpm -= 10;
    
    for (targetRpm; targetRpm >= 0; targetRpm -= 10) {
        if (targetRpm < 0) targetRpm = 0;
        leftPID.Compute();
        rightPID.Compute();
        currentL -= (speedOffsetLeft * 1.05);
        currentL = min(currentL, 350);
        currentR -= (speedOffsetRight); 
        currentR = min(currentR, 350);
        md.setSpeeds(dirR * currentR, dirL * currentL);   
        delay(25);      
        rpml = max(round(60000000.0 / (pulseTimeLeft * 562.25)), 0);
        rpmr = max(round(60000000.0 / (pulseTimeRight * 562.25)), 0);
    }
    
    setupPID(false);
    Serial.println("OK");
    md.setSpeeds(0, 0);
}

void turn(boolean left, int angle) {
    int dirL = left? -1 : 1;
    int dirR = left? 1 : -1;    
    wavesThreshold = round(4.7 * angle) - 71;
    reset();
    setupPID(true);
    
    while (enabled) {
        leftPID.Compute();
        rightPID.Compute();
        currentL -= (speedOffsetLeft * 1.05);
        currentL = min(currentL, 350);
        currentR -= (speedOffsetRight); 
        currentR = min(currentR, 350);
        md.setSpeeds(dirR * currentR, dirL * currentL);   
        delay(25);  
        rpml = max(round(60000000.0 / (pulseTimeLeft * 562.25)), 0);
        rpmr = max(round(60000000.0 / (pulseTimeRight * 562.25)), 0);
        if (pulseTimeLeft == 0) rpml = 0;
        if (pulseTimeRight == 0) rpmr = 0;
        if (targetRpm < 60) targetRpm += 5;
    }
    
    brake(dirR, dirL);
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
    
    if (dist <= 20 && average >= t10) {
        dist = 10;
    }

    if (sensorDistance[index] == 10 && dist <= 20) {
        return 10;
    }
    
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
            int dist = distanceStr.toInt();
            moveRobot(1, dist, 60);
        }  
    } else if (inputString.indexOf("r") == 0) {        
        String distanceStr = inputString.substring(1);
        
        if (isNumber(distanceStr)) {
            int dist = distanceStr.toInt();
            moveRobot(-1, dist, 60);
        }   
    } else if (inputString.indexOf("tl") == 0) {
        String angleStr = inputString.substring(2);
        
        if (isNumber(angleStr)) {
            int angle = angleStr.toInt();
            turn(true, angle);
        }
    } else if (inputString.indexOf("tr") == 0) {
        String angleStr = inputString.substring(2);
        
        if (isNumber(angleStr)) {
            int angle = angleStr.toInt();
            turn(false, angle);
        } 
    }

    if (inputString.indexOf("reset") == 0) {
        obstacleAhead = false;
        Serial.println("OK");
    }
}

void moveRobot(int dir, int dist, int rpm) {
    // Movement Distance
    // Theoretically -> 1  rotation = 18.8495559 cm (Wheel circumference * pi = 6cm * pi)
    // 10cm -> 10 / 18.8495559 = 0.53051648 rotation
    // 1 rotation -> 562.25 waves
    // 0.53051648 rotation -> 298.28289 waves
    // Alternatively, calculate using timing? Might be inaccurate.
    
    double multiplier = 1.0 / (60.0 / (60000.0 / ((rpm / 5.0) * 25)));
    int offset = round(562.25 / multiplier);
    results(dir, dist, offset);
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
    if (wavesLeft >= wavesThreshold) enabled = false;
}

boolean isNumber(String str) {
    for (byte i = 0; i < str.length(); i++) {
        if (!isDigit(str.charAt(i))) return false;
    }

    return true;
}
