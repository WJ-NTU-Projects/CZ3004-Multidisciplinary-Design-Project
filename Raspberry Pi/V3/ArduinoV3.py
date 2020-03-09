import serial
import threading
import time

class ArduinoV3:
    def __init__(self):
        self.baudrate = 115200
        self.serial = 0
        self.connected = False

    def connect(self):
        try:
            self.serial = serial.Serial("/dev/ttyACM0", self.baudrate, write_timeout = 0)
            print("Connected to Arduino 0 successfully.")
            self.connected = True
            return 1
        except:
            try:
                self.serial = serial.Serial("/dev/ttyACM1", self.baudrate, write_timeout = 0)
                print("Connected to Arduino 1 successfully.")
                self.connected = True
                return 1
            except Exception as e2:
                print("Failed to connect to Arduino: %s" %str(e2))
                self.connected = False
                return 0

    def readThread(self, pc, android):
        while True:
            try:
                message = self.serial.readline()
                print("Read from Arduino: %s" %str(message))

                if len(message) <= 1:
                    continue

                if (message[0] == 80):
                    pc.write(message[1:])
                    continue

                if (message[0] == 68):
                    android.write(message[1:])
                    continue
            except Exception as e:
                print("Failed to read from Arduino: %s" %str(e))
                self.connected = False
                return

    def write(self, message):
        try:
            self.serial.write(message)
            print("Write to Arduino: %s" %str(message))
            print()
        except Exception as e:
            print("Failed to write to Arduino: %s" %str(e))
                        


