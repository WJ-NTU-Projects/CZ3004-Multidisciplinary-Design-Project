import serial

class ArduinoV2:
    def __init__(self):
        self.baudrate = 115200
        self.serial = 0

    def connect(self):
        try:
            self.serial = serial.Serial("/dev/ttyACM0", self.baudrate)
            print("Connected to Arduino successfully.")
            return 1
        except Exception as e:
            try:
                self.serial = serial.Serial("/dev/ttyACM1", self.baudrate)
                print("Connected to Arduino successfully.")
                return 1
            except Exception as e2:
                print("Failed to connect to Arduino: %s" %str(e2))
                return 1

    def write(self, message):
        try:
            self.serial.write(message)
        except Exception as e:
            print("Failed to write to Arduino: %s" %str(e))

    def read(self):
        try:
            message = self.serial.readline()
            return message
        except Exception as e:
            print("Failed to read from Arduino: %s" %str(e))

