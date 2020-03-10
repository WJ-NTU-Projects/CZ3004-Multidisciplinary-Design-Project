import _thread
import os

from ArduinoV3 import *
from PCV3 import *
from AndroidV3 import *

class Main:
    def __init__(self):
        os.system("sudo hciconfig hci0 piscan")
        print("Please wait...")
        self.arduino = ArduinoV3()
        self.pc = PCV3()
        self.android = AndroidV3()

    def test(self):
        self.pc.connectImg()
        
        while True:
            if self.arduino.connected == False:
                result = self.arduino.connect()

                if (result == 0): 
                    continue

                try:
                    _thread.start_new_thread(self.arduino.readThread, (self.pc, self.android))
                    print("Arduino thread started.")
                except Exception as e:
                    print("Arduino threading error: %s" %str(e))

            if self.pc.connected == False:
                self.pc.connect()

                if self.pc.connected == True:
                    try:
                        _thread.start_new_thread(self.pc.readThread, (self.arduino, self.android))
                        print("PC thread started.")
                    except Exception as e:
                        print("PC threading error: %s" %str(e))

            if self.android.connected == False:
                self.android.connect()

                if self.android.connected == True:
                    try:
                        _thread.start_new_thread(self.android.readThread, (self.arduino, self.pc))
                        print("Android thread started.")
                    except Exception as e:
                        print("Android threading error: %s" %str(e))

    def disconnectAll(self):
        try:
            self.android.disconnect()
            self.pc.disconnect()
        except:
            pass

if __name__ == "__main__":
    a = Main()
    
    try:
        a.test()
    except KeyboardInterrupt:
        print("Terminating program...")
        a.disconnectAll()
    

