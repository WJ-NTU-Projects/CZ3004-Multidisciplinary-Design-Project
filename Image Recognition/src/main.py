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
    global img_count, graph, predictions, images, uniquePreds, areas
    content = request.data
    frame = pickle.loads(content) # get serialized data
    cv2.imwrite("../raw/img"+str(img_count)+".jpg", frame)
    pred, file, area, pos = run(frame, graph, cnn, img_count)
    predictions.append(pred)
    if pred not in uniquePreds:
        images.append(file)
        uniquePreds.add(pred)
        areas[pred] = [img_count, area, pos]
        print("Detected", pred)
    elif pred > 0:
        temp_list = areas.get(pred)
        if area > temp_list[1]:
            areas[pred] = [img_count, area, pos]
    img_count+=1
    return ('', 204) # return a no content response

# Endpoint to send classification results to algo team
@app.route('/end', methods=['GET'])
def finished():
    global predictions, images
    positions = []
    new_preds = [-1 for i in range(len(predictions))]
    for pred, temp in areas.items():
        new_preds[temp[0]] = pred
    for pred in new_preds:
        if pred > 0:
            positions.append(areas.get(pred)[2])
    print(json.dumps(new_preds)+";"+json.dumps(positions))
    threading.Thread(target=plotImages, args=(images,)).start()
    return json.dumps(new_preds)+";"+json.dumps(positions)

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
    global img_count, graph, predictions, images, areas, uniquePreds
    import os
    files = os.listdir("../raw/")
    files = sorted(files, key=lambda x: int(x[3:-4]))
    for f in files:
        frame = cv2.imread("../raw/"+f)
        pred, file, area, pos = run(frame, graph, cnn, img_count)
        predictions.append(pred)
        if pred not in uniquePreds:
            images.append(file)
            uniquePreds.add(pred)
            areas[pred] = [img_count, area, pos]
            print("Detected", pred)
        elif pred > 0:
            temp_list = areas.get(pred)
            if area > temp_list[1]:
                areas[pred] = [img_count, area, pos]
        img_count+=1

# for debug
def debugEnd(images):
    positions = []
    new_preds = [-1 for i in range(len(predictions))]
    for pred, temp in areas.items():
        new_preds[temp[0]] = pred
    for pred in new_preds:
        if pred > 0:
            positions.append(areas.get(pred)[2])
    print(json.dumps(new_preds)+";"+json.dumps(positions))
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
    areas = {}
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