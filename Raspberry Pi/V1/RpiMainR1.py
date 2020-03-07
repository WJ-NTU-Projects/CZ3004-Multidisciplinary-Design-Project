import queue
import _thread
import threading
import os


from Arduino import *
from Android import *
from Pc import *
from params import *
from sendImg import *



class Main:

    def __init__(self):
        # allow rpi bluetooth to be discoverable
        os.system("sudo hciconfig hci0 piscan")

        # initialize connections
        self.bt = Android()
        self.ard = Arduino()
        self.pc = Pc()
        self.pc.connect()
        self.bt.connect(uuid)
        self.ard.connect()


        # initialize sendImg
#		self.sendImg = sendImg()

        # initialize queues
        self.btQueue = queue.Queue(maxsize=0)
        self.ardQueue = queue.Queue(maxsize=0)
        self.pcQueue = queue.Queue(maxsize=0)

        print ("===========================")
        print ("===Starting Transmission===")
        print ("===========================")

    # read/write from Android (Bluetooth)
    def readBt(self, pcQueue, ardQueue):
        while 1:
            msg = self.bt.read()
#        		pcQueue.put_nowait(msg)
            strmsg = msg.decode('utf-8')
            if len(strmsg)!=0:
                newmsg = strmsg[1:]
                if strmsg[0] == "A":
                    print ("Read from Bluetooth: %s\n" % msg)
                    ardQueue.put_nowait(bytes(newmsg+'\n', 'utf-8'))
                elif strmsg[0] == "P":
                    print ("Read from Bluetooth: %s\n" % msg)
                    pcQueue.put_nowait(bytes(newmsg+'\n', 'utf-8'))
                elif strmsg[0] == "Z":
                    print ("Read from Bluetooth: %s\n" % msg)
                    pcQueue.put_nowait(bytes(newmsg+'\n', 'utf-8'))
                    ardQueue.put_nowait(bytes(newmsg+'\n', 'utf-8'))
#            else:
#               print("Read from Bluetooth:%s\n" %
#                msg+" incorrect format received")
#                pass

    def writeBt(self, btQueue):
        while 1:
            if not btQueue.empty():
                msg = btQueue.get_nowait()
                self.bt.write(msg)
                print ("Write to Bluetooth: %s\n" % msg)

    # read/write from Arduino (Serial comm)
    def readArd(self, pcQueue, btQueue):
        while 1:
            msg = self.ard.read()
            strmsg = msg.decode('utf-8')
            if len(strmsg)!=0:
                if strmsg[0] == "D":
                    print ("Read from serial: %s\n" % msg)
                    btQueue.put_nowait(msg[1:])
                elif strmsg[0] == "P":
                    print ("Read from serial: %s\n" % msg)
                    pcQueue.put_nowait(msg[1:])
                elif strmsg[0] == "Z":
                    print ("Read from serial: %s\n" % msg)
                    btQueue.put_nowait(msg[1:])
                    pcQueue.put_nowait(msg[1:])

#            else:
#                print("Read from serial: %s\n" %
#               msg+" incorrect format received")
#               pass

    def writeArd(self, ardQueue):
        while 1:
            if not ardQueue.empty():
                msg = ardQueue.get_nowait()
                self.ard.write(msg)
                print ("Write to Serial: %s\n" % msg)

    # read/write from PC (Serial comm)
    def readPc(self, ardQueue, btQueue):
        with open("pc_log.txt","w",newline="") as f:
            f.write("start\n")
        while 1:
            msg = self.pc.read()
            strmsg = msg.decode('utf-8')
            if len(strmsg)!=0:
                msgs = strmsg.split('\r\n')
                with open("pc_log.txt", "a", newline="") as f:
                    for m in msgs:
                        if m is not None:
                            f.write(m+"\n")
                for msg in msgs:
                    newmsg = msg[1:]+'\r\n'
                    if strmsg[0] == "A":
                        if strmsg[1]=="M" or strmsg[1]=="L" or strmsg[1]=="R" or strmsg[1]=="T":
                            threading.Thread(target=self.sendImg.run).start() # start image recog thread
                        print ("Read from WIFI: %s\n" % msg)
                        ardQueue.put_nowait(bytes(newmsg, 'utf-8'))
                    elif strmsg[0] == "D":
                        print ("Read from WIFI: %s\n" % msg)
                        btQueue.put_nowait(bytes(newmsg, 'utf-8'))
                    elif  strmsg[0] == "Z":
                        print ("Read from WIFI: %s\n" % msg)
                        btQueue.put_nowait(bytes(newmsg, 'utf-8'))
                        ardQueue.put_nowait(bytes(newmsg, 'utf-8'))

#           else:
#                print("Read from WIFI: %s\n" %
#                         msg+" incorrect format received")
#                  pass

    def writePc(self, pcQueue):
        while 1:
            if not pcQueue.empty():
                msg = pcQueue.get_nowait()
            #	len1=len(msg)
            #	msg= "hello world"
                if not (msg is None):
                    self.pc.write(msg)
                print ("Write over WIFI : %s\n" % msg)
            #	print "Length: %d\n" %len1

    # multithreading
    def multithread(self):
        try:
            _thread.start_new_thread(
                self.readBt, (self.pcQueue, self.ardQueue))
            _thread.start_new_thread(
                self.readArd, (self.pcQueue, self.btQueue))
            _thread.start_new_thread(
                self.readPc, (self.ardQueue, self.btQueue))
            _thread.start_new_thread(self.writeBt, (self.btQueue,))
            _thread.start_new_thread(self.writeArd, (self.ardQueue,))
            _thread.start_new_thread(self.writePc, (self.pcQueue,))
            

        except Exception as e:
            print ("Error in threading: %s" % str(e))

        while 1:
            pass

    def disconnectAll(self):
        try:
            self.bt.disconnect()
# 			self.ard.disconnect()
            self.pc.disconnect()
        except Exception as e:
            pass

if __name__ == "__main__":
    test=Main()
    try:
        while True:
#            test.testPC()
            test.multithread()
    except KeyboardInterrupt:
        print ("Terminating the program now...")
        test.disconnectAll()
        pass


