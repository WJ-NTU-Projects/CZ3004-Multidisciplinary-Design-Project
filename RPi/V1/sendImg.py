import socket, time, pickle, requests
from picamera import PiCamera
from picamera.array import PiRGBArray
from params import WIFI_IP, IMGREG_PORT

class sendImg:
  host=WIFI_IP
  port=IMGREG_PORT

  def __init__(self, host=WIFI_IP, port=IMGREG_PORT):
    self.host = host
    self.port = port
    self.count = 0
    self.serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    self.serversocket.bind((self.host, self.port))
    self.serversocket.listen(1) #only connect up to 1 request

    # camera initialisation
    self.camera = PiCamera()
    self.camera.resolution = (640, 480)
    self.output = PiRGBArray(self.camera)

    # accept connection and extract IP address
    (self.clientsocket, self.address) = self.serversocket.accept()
    self.cleanUp()

  def cleanUp(self):
    # free resources
    self.clientsocket.close()
    self.serversocket.close()

  def run(self):
    # let camera warm-up
    count = 0
    self.camera.start_preview()
    time.sleep(2)

    while True:
      self.camera.capture(self.output, 'rgb')
      img_arr = self.output.array
      data = pickle.dumps(img_arr)
      # send to PC/Laptop via HTTP POST
      r = requests.post("http://"+str(self.address[0])+":8123", data=data)
      print('Image', count , 'sent')
      self.output.truncate(0)
      count += 1
      time.sleep(0.5)

  # this functions take 2 pictures every time the robot moves
  def takeTwice(self):
    for i in range(2):
      self.camera.capture(self.output, 'rgb')
      frame = self.output.array
      data = pickle.dumps(frame)
      # send to Laptop via HTTP POST
      r = requests.post("http://"+str(self.address[0])+":8123", data=data)
      print("Image", self.count, "sent")
      self.output.truncate(0)
      self.count+=1
