import socket
import sys

class PCV2:
    host = "192.168.16.16"
    port = 9123

    def __init__(self):
        self.host = "192.168.16.16"
        self.port = 9123
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        print("Socket established successfully.")

        try:
            self.socket.bind((self.host, self.port))
            print("Socket binded successfully.")
        except socket.error as e:
            print("Socket binding failed: %s" %str(e))
            sys.exit()

    def connect(self):
        self.socket.listen(3)
        print("Waiting for PC connection...")
        self.client_socket, self.address = self.socket.accept()
        print("PC connected successfully.")

    def disconnect(self):
        try:
            self.socket.close()
            print("Disconnected from PC successfully.")
        except Exception as e:
            print("Failed to disconnect from PC: %s" %str(e))

    def write(self, message):
        try:
            self.client_socket.sendto(message, self.address)
        except ConnectionResetError as ce:
            self.connect()
        except socket.error as e:
            sys.exit()
        except IOError as e2:
            pass

    def read(self):
        try:
            message = self.client_socket.recv(1024)
            return message
        except socket.error as e:
            sys.exit()
        except IOError as e2:
            pass