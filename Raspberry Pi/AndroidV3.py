from bluetooth import *
import time

class AndroidV3:
    def __init__(self):
        self.uuid = "00001101-0000-1000-8000-00805F9B34FB"
        self.connected = False

    def connect(self):
        try:
            self.server_socket = BluetoothSocket(RFCOMM)
            self.server_socket.bind(("", 4))
            self.server_socket.listen(1)
            port = self.server_socket.getsockname()[1]
            advertise_service(self.server_socket, "SampleServer", service_id = self.uuid, service_classes = [self.uuid, SERIAL_PORT_CLASS], profiles = [SERIAL_PORT_PROFILE],)
            print("Waiting for Android connection...")
            self.client_socket, client_info = self.server_socket.accept()
            print("Connected to Android successfully.")
            self.connected = True
            return 1
        except Exception as e:
            print("Failed to connect to Android: %s" %str(e))
            
            try:
                self.client_socket.close()
                self.server_socket.close()
            except:
                print("ERROR")
            return 0

    def disconnect(self):
        try:
            self.client_socket.close()
            self.server_socket.close()
            self.socket = -1
            print("Disconnected from Android successfully.")
        except Exception as e:
            print("Failed to disconnect from Android: %s" %str(e))
        self.connected = False

    def readThread(self, arduino, pc):
        while True:
            try:
                message = self.client_socket.recv(1024)
                print("Read from Android: %s" %str(message))

                if len(message) <= 1:
                    continue

                if (message[0] == 65):
                    arduino.write(message[1:] + '\n'.encode("utf-8"))
                    continue

                if (message[0] == 80):
                    pc.write(message[1:] + '\n'.encode("utf-8"))
                    continue

            except Exception as e:
                print("Failed to read from Android: %s" %str(e))
                self.disconnect()
                return

    def write(self, message):
        try:
            self.client_socket.send(message)
            print("Write to Android: %s" %str(message))
            print()
        except Exception as e:
            print("Failed to write to Android: %s" %str(e))
            self.disconnect()