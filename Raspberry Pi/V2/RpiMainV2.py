import _thread
import queue
import os
import sys

from ArduinoV2 import *
from PCV2 import *
from AndroidV2 import *

class Main:
    def __init__(self):
        os.system("sudo hciconfig hci0 piscan")

        print("Please wait...")
        self.arduino = ArduinoV2()
        self.pc = PCV2()
        self.android = AndroidV2()
        self.arduino.connect()
        self.pc.connect()
        self.android.connect()
        self.arduinoQueue = queue.Queue(maxsize = 0)
        self.pcQueue = queue.Queue(maxsize = 0)
        self.androidQueue = queue.Queue(maxsize = 0)

    def readFromArduino(self, pcQueue, androidQueue):
        while True:
            message = self.arduino.read()

            if len(message) > 1:
                if (message[0] == 80):
                    pcQueue.put_nowait(message[1:])
                    continue

                if (message[0] == 68):
                    androidQueue.put_nowait(message[1:])
                    continue


    def writeToArduino(self, arduinoQueue):
        while True:
            if not arduinoQueue.empty():
                message = arduinoQueue.get_nowait()
                self.arduino.write(message)

    def readFromPC(self, arduinoQueue, androidQueue):
        while True:
            message = self.pc.read()

            if len(message) > 1:
                if (message[0] == 65):
                    arduinoQueue.put_nowait(message[1:] + "\n".encode("utf-8"))
                    continue

                if (message[0] == 68):
                    androidQueue.put_nowait(message[1:] + "\n".encode("utf-8"))
                    continue

    def writeToPC(self, pcQueue):
        while True:
            if not pcQueue.empty():
                message = pcQueue.get_nowait()
            
                if not (message is None):
                    self.pc.write(message)

    def readFromAndroid(self, arduinoQueue, pcQueue):
        while True:
            message = self.android.read()

            if len(message) > 1:
                if (message[0] == 65):
                    arduinoQueue.put_nowait(message[1:] + "\n".encode("utf-8"))
                    continue

                if (message[0] == 80):
                    pcQueue.put_nowait(message[1:] + "\n".encode("utf-8"))
                    continue

    def writeToAndroid(self, androidQueue):
        while True:
            if not androidQueue.empty():
                message = androidQueue.get_nowait()

                if not (message is None):
                    self.android.write(message)

    def multithread(self):
        try:
            _thread.start_new_thread(self.readFromArduino, (self.pcQueue, self.androidQueue))
            _thread.start_new_thread(self.readFromPC, (self.arduinoQueue, self.androidQueue))
            _thread.start_new_thread(self.readFromAndroid, (self.arduinoQueue, self.pcQueue))
            _thread.start_new_thread(self.writeToArduino, (self.arduinoQueue,))
            _thread.start_new_thread(self.writeToPC, (self.pcQueue,))
            _thread.start_new_thread(self.writeToAndroid, (self.androidQueue,))
            print("Ready!")
        except Exception as e:
            print("Threading error: %s" %str(e))
            sys.exit()

        while True:
            pass

    def disconnectAll(self):
        try:
            self.pc.disconnect()
            self.android.disconnect()
        except Exception as e:
            pass
    
if __name__ == "__main__":
    test = Main()
    
    try:
        test.multithread()
    except KeyboardInterrupt:
        print("Terminating program...")
        test.disconnectAll()