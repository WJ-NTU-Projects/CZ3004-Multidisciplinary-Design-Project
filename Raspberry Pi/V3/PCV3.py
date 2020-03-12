import socket
import select
import sys
import threading
from sendImg import *

class PCV3:
    def __init__(self):
        self.host = "192.168.16.16"
        self.port = 9123
        self.connected = False

    def connect(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        print("Socket established successfully.")

        try:
            self.socket.bind((self.host, self.port))
            print("Socket binded successfully.")
        except socket.error as e:
            print("Socket binding failed: %s" %str(e))
            sys.exit()

        self.socket.listen(3)
        print("Waiting for PC connection...")
        self.client_socket, self.address = self.socket.accept()
        print("PC connected successfully.")
        self.connected = True

    def connectImg(self):
        self.sendImg = sendImg()

    def disconnect(self):
        try:
            self.socket.close()
            self.socket = -1
            print("Disconnected from PC successfully.")
        except Exception as e:
            print("Failed to disconnect from PC: %s" %str(e))
        self.connected = False

    def readThread(self, arduino, android):
        while True:
            try:
                messages = self.client_socket.recv(1024)

                if not messages:
                    print("PC disconnected remotely.")
                    self.disconnect()
                    return

                test = messages.split(b'\r\n')
                
                for message in test:
                    print("Read from PC: %s" %str(message))

                    if len(message) <= 1:
                        continue

                    if (message[0] == 65):
                        arduino.write(message[1:] + '\n'.encode("utf-8"))
                        command = message[1]

                        #if (command == 77 or command == 76 or command == 82 or command == 84):
                        #    threading.Thread(target= self.sendImg.takeTwice).start()
                        
                        continue

                    if (message[0] == 68):
                        android.write(message[1:] + '\n'.encode("utf-8"))
                        continue
            except socket.error as e:
                print("Failed to read from PC: %s" %str(e))
                self.disconnect()
                return
            except IOError as ie:
                print("Failed to read from PC: %s" %str(ie))
            except Exception as e2:
                print("Failed to read from PC: %s" %str(e2))
                self.disconnect()
                return


    def write(self, message):
        try:
            self.client_socket.sendto(message, self.address)
            print("Write to PC: %s" %str(message))
            print()
        except ConnectionResetError:
            self.disconnect()
        except socket.error:
            self.disconnect()
        except IOError as e:
            print("Failed to write to PC: %s" %str(e))