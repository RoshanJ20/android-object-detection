package com.example.objectdetection_inventorysystem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.model.Model;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ImageProcessor;

import android.content.Context;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import android.content.res.AssetFileDescriptor;
import com.example.objectdetection_inventorysystem.ObjectDetectionModel;


public class ObjectDetectionModel {
    private static final int MODEL_INPUT_SIZE = 224; // Input size for the model
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;

    private Interpreter tflite;
    private final TensorImage inputImageBuffer;
    private final int[] imageShape; // Adjust based on your model's input

    // Optional GPU delegate for acceleration

    public ObjectDetectionModel(Context context, String modelPath) throws IOException {
        // Load the model
        tflite = new Interpreter(loadModelFile(context, modelPath));
        inputImageBuffer = new TensorImage();
        imageShape = tflite.getInputTensor(0).shape(); // Assuming single input. Adjust as necessary.
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    public void runInference(Bitmap bitmap) {
        if (bitmap == null) {
            return; // Bitmap is not valid
        }

        // Preprocess the image
        Bitmap resizedImage = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true);
        inputImageBuffer.load(resizedImage);
        TensorImage processedImage = preprocessImage(inputImageBuffer);

        // Run inference
        float[][] output = new float[1][imageShape[1]]; // Adjust based on your model's output
        tflite.run(processedImage.getBuffer(), output);

        // Post-process and use the output as needed
        int detectedClass = argMax(output[0]);
        float confidence = output[0][detectedClass];
        // Here, handle the detected class and confidence as needed
    }

    private TensorImage preprocessImage(TensorImage image) {
        // Assuming MODEL_INPUT_SIZE is the required size for your model
        int cropSize = Math.min(image.getWidth(), image.getHeight());
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize)) // Crop to square
                        .add(new ResizeOp(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                        .build();
        return imageProcessor.process(image);
    }


    private int argMax(float[] array) {
        int argmax = 0;
        float max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                argmax = i;
                max = array[i];
            }
        }
        return argmax;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}
