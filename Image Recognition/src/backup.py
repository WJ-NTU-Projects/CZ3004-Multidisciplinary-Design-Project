from flask import Flask, request
import pickle, json, cv2, math, threading
from imgReg import run
import tensorflow as tf
from cnn import CNN
import matplotlib.pyplot as plt

img_count = 0 # to assign image name
cnn = CNN("gray")
graph = tf.get_default_graph() # to tackle thread issues
app = Flask(__name__)

# Endpoint to receive image data then localizes and classifies images
@app.route('/', methods=['POST'])
def receiveImage():
    global img_count, graph, predictions, images, xes, uniquePreds
    content = request.data
    frame = pickle.loads(content) # get serialized data
    cv2.imwrite("../raw/img"+str(img_count)+".jpg", frame)
    pred, file, x = run(frame, graph, cnn, img_count)
    img_count += 1
    if pred not in uniquePreds:
        images.append(file)
        predictions.append(pred)
        uniquePreds.add(pred)
        print("Detected", pred)
        if x < 200:
            xes.append("l")
        elif x > 440:
            xes.append("r")
        else:
            xes.append("c")
    else:
        predictions.append(-1)
    return ('', 204) # return a no content response

# Endpoint to send classification results to algo team
@app.route('/end', methods=['GET'])
def finished():
    global predictions, images, xes
    print(json.dumps(predictions)+";"+json.dumps(xes))
    threading.Thread(target=plotImages, args=(images,)).start()
    return json.dumps(predictions)+";"+json.dumps(xes)

def plotImages(images):
    toPlot = []
    for file in images:
        img = cv2.imread(file)
        toPlot.append(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    _, axs = plt.subplots(math.ceil(len(toPlot)/3), 3, gridspec_kw = {'wspace':0, 'hspace':0}, figsize=(100,100))
    for img, ax in zip(toPlot, axs.flatten()):
        ax.imshow(img)
        ax.set_xticklabels([])
        ax.set_yticklabels([])
    plt.show()
    import os
    os._exit(1)

# for debug
def forDebug():
    global img_count, graph, predictions, images, xes, uniquePreds
    import os
    files = os.listdir("../raw/")
    files = sorted(files, key=lambda x: int(x[3:-4]))
    for f in files:
        frame = cv2.imread("../raw/"+f)
        pred, file, x = run(frame, graph, cnn, img_count)
        img_count+=1
        # predictions.append(pred)
        if pred not in uniquePreds:
            images.append(file)
            predictions.append(pred)
            uniquePreds.add(pred)
            print("Detected", pred)
            if x < 200:
                xes.append("l")
            elif x > 440:
                xes.append("r")
            else:
                xes.append("c")
        # if x is not None:
        #     if x < 200:
        #         xes.append("l")
        #     elif x > 440:
        #         xes.append("r")
        #     else:
        #         xes.append("c")
        else:
           predictions.append(-1)

# for debug
def debugEnd(images):
    print(json.dumps(predictions)+";"+json.dumps(xes))
    toPlot = []
    for file in images:
        img = cv2.imread(file)
        toPlot.append(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    _, axs = plt.subplots(math.ceil(len(toPlot)/3), 3, gridspec_kw = {'wspace':0, 'hspace':0}, figsize=(100,100))
    for img, ax in zip(toPlot, axs.flatten()):
        ax.imshow(img)
        ax.set_xticklabels([])
        ax.set_yticklabels([])
    plt.show()

if __name__ == '__main__':
    predictions = []
    images = []
    xes = []
    uniquePreds = set([-1])
    app.run(host='0.0.0.0', port=8123)

    # forDebug()
    # debugEnd(images)

    # RPI = "192.168.16.16"
    # TEMP_PORT = 8125
    # MY_IP = socket.gethostbyname(socket.getfqdn()) # get my IP address
    # print(MY_IP)
    # s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # while True:
    #     try:
    #         s.connect((RPI, TEMP_PORT)) # to establish connection with RPI
    #         break
    #     except ConnectionRefusedError:
    #         print("Connection failed. Retrying...")
    #         continue
    # s.close() # close socket