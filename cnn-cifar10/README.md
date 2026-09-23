# CNN CIFAR-10

A convolutional network for CIFAR-10 in Keras. Three Conv2D blocks with max
pooling and dropout, a dense layer and a softmax over the 10 classes. It
trains for 50 epochs, saves the model every 5 epochs and at the end plots how
the predictions on the same 10 test images improve.

Pattern Recognition and Machine Learning course, assignment 2.

TensorFlow needs Python 3.10 to 3.13.

    pip install tensorflow keras matplotlib numpy
    python CNN.py
