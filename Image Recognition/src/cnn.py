from keras.models import Model
from keras.layers import Input, Dense, Conv2D, BatchNormalization, Flatten, MaxPooling2D, Lambda, ReLU
from keras.optimizers import Adam
import joblib
import numpy as np

class CNN:
    def __init__(self, version):
        self.dim = 64
        self.lb = self.getLabelBinarizer()
        self.version = version
        if self.version == "gray":
            self.channels = 1
            self.model = self.getArchitecture()
            self.model.load_weights("./cnn_weights_gray.h5") # load saved weights
        else:
            self.channels = 3
            self.model = self.getArchitecture()
            self.model.load_weights("./cnn_weights_colour.h5") # load saved weights

    # returns probability and class
    def predict(self, graph, frame):
        img = np.reshape(frame, newshape=(1, self.dim, self.dim, self.channels))
        with graph.as_default():
            pred = self.model.predict(img)
            return (self.lb.classes_[pred.argmax(axis=-1).item()], np.max(pred))
    
    def getLabelBinarizer(self):
        return joblib.load("labelbinarizer.joblib")

    # graph architecture of cnn
    def getArchitecture(self):
        input_layer = Input(shape=(self.dim, self.dim, self.channels))
        x = Conv2D(32, (3,3), padding="same")(input_layer)
        x = BatchNormalization()(x)
        x = ReLU()(x)
        x = Conv2D(32, (3,3), padding="same")(x)
        x = BatchNormalization()(x)
        x = ReLU()(x)
        x = MaxPooling2D()(x)

        x = Conv2D(64, (5,5), padding="same")(x)
        x = BatchNormalization()(x)
        x = ReLU()(x)
        x = Conv2D(64, (5,5), padding="same")(x)
        x = BatchNormalization()(x)
        x = ReLU()(x)
        x = MaxPooling2D()(x)

        x = Conv2D(128, (5,5), padding="same")(x)
        x = BatchNormalization()(x)
        x = ReLU()(x)
        x = Conv2D(128, (5,5), padding="same")(x)
        x = BatchNormalization()(x)
        x = ReLU()(x)
        x = MaxPooling2D()(x)

        x = Conv2D(256, (5,5), padding="same")(x)
        x = BatchNormalization()(x)
        x = ReLU()(x)
        x = Conv2D(256, (5,5), padding="same")(x)
        x = BatchNormalization()(x)
        x = ReLU()(x)
        x = MaxPooling2D()(x)

        x = Flatten()(x)
        x = Dense(512, activation="relu")(x)
        output = Dense(15, activation="softmax")(x)

        # Connect the inputs with the outputs
        cnn = Model(inputs=input_layer,outputs=output)
        cnn.compile(loss="categorical_crossentropy",optimizer=Adam(lr=0.00006, decay=1e-6))
        return cnn