package cn.dmrf.nuaa.gesturewithtf.Utils;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileLock;
import java.util.ArrayList;

public class TensorFlowUtil {

    private TensorFlowInferenceInterface inferenceInterface;

    private String input_cnn = "input";
    private String fullconnection1 = "fullconnection1";
    private String input_lstm = "input_lstm";
    private String output_lstm = "output_lstm";
    private String cnn_accuracy = "softmax";
    private String lstm_accuracy = "softmax_lstm";
    //存储cnn和lstm的可信度
    private float[] cnn_softmax;//大小为13
    private float[] lstm_softmax;//大小为6

    private String[] outputNames;
    private String[] outputNames2;
    private float[] floatValues;
    private float[] outputs;
    private long[] outputint;
    private float[] outputfuuconnection;
    private int classes = 6;
    private int w = 2200;
    private int h = 8;
    private int c = 2;
    private int batch = 1;
    private boolean logStats = true;
    private AssetManager assetManager;
    private String target = "H";

    public TensorFlowUtil(AssetManager assetManager, String model) {
        try {
            this.assetManager = assetManager;
            inferenceInterface = new TensorFlowInferenceInterface(assetManager, model);
            outputNames = new String[]{fullconnection1};
            //如果需要cnn部分的置信度
            //outputNames = new String[]{fullconnection1，cnn_accuracy};
            //outputNames2 = new String[]{output_lstm};
            //如果需要lstm部分的置信度
            outputNames2 = new String[]{output_lstm, lstm_accuracy};

            floatValues = new float[w * h * c];

            outputs = new float[classes];
            outputfuuconnection = new float[256];

            outputint = new long[1];
            cnn_softmax = new float[13];
            lstm_softmax = new float[6];
            PredictContinousTest();


        } catch (Exception e) {
            System.out.println("模型加载失败");
        }
    }

    @SuppressLint("LongLogTag")
    public long PredictContinousTest() {
        float test[] = new float[8800];

        outputint[0] = -1;
        inferenceInterface.feed(input_cnn, test, batch, 8, 550, 2);

        inferenceInterface.run(outputNames, logStats);

        Trace.beginSection("fetch");
        inferenceInterface.fetch(fullconnection1, outputfuuconnection);
        Log.i("TensorflowesturePredict", "result:" + outputint[0]);
        return outputint[0];
    }


    @SuppressLint("LongLogTag")
    public long PredictContinous(float[][] gesturedata, int count) {
        float accuracymax = 0;
        float input_ls[] = new float[1024];
        for (int i = 0; i < 1024; i++) {
            input_ls[i] = 0;
        }
        float mic_gesture[] = new float[8800];
        for (int i = 0; i <= count; i++) {

            for (int j = 0; j < 8800; j++) {
                mic_gesture[j] = gesturedata[i][j];
            }
            inferenceInterface.feed(input_cnn, mic_gesture, 1, 8, 550, 2);
            inferenceInterface.run(outputNames, logStats);
            inferenceInterface.fetch(fullconnection1, outputfuuconnection);
            //执行这句之后cnn_softmax里面就是当前cnn网络根据送入的数据预测出来的各组label的可信度，存储在cnn_softmax中，每一个下标中存储的是
            //对应label的概率
            //inferenceInterface.fetch(cnn_accuracy, cnn_softmax);
            for (int k = i * 256; k < (i + 1) * 256; k++) {
                input_ls[k] = outputfuuconnection[k % 256];
            }
        }

        inferenceInterface.feed(input_lstm, input_ls, 1, 1024, 1, 1);
        inferenceInterface.run(outputNames2, logStats);
        inferenceInterface.fetch(output_lstm, outputint);
        //执行下面这一句之后拿到的识别lstm的可信度，lstm_softmax六个位置存储6个label的概率
        inferenceInterface.fetch(lstm_accuracy, lstm_softmax);
        for (int i = 0; i < 6; i++) {
            if (lstm_softmax[i] > accuracymax)
                accuracymax = lstm_softmax[i];
        }
        if (accuracymax > 0.9) {
           // Log.i("TensorflowesturePredict", "result:" + outputint[0]);
            return outputint[0];
        }
        else
            return -1;
    }

    @SuppressLint("LongLogTag")
    public void PredictTest() {
        try {
            InputStream is = assetManager.open(target + "_I.txt");
            ArrayList<Float> id = readTextFromFile(is);
            InputStream is2 = assetManager.open(target + "_Q.txt");
            ArrayList<Float> qd = readTextFromFile(is2);

            float dataraw[][][] = new float[8][550][2];

            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 550; j++) {
                    for (int k = 0; k < 2; k++) {
                        if (k == 0) {
                            dataraw[i][j][k] = id.get(i * 550 + j);
                        } else {
                            dataraw[i][j][k] = qd.get(i * 550 + j);
                        }

                    }
                }
            }
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 550; j++) {
                    for (int k = 0; k < 2; k++) {
                        floatValues[k + j * 2 + i * 1100] = dataraw[i][j][k];
                    }
                }
            }


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (int i = 0; i < 13; i++) {
            outputs[i] = -1;
        }
        // 把数据喂给TensorFlow
        Trace.beginSection("feed");
        //  inferenceInterface.feed(inputName, floatValues, batch, h, w, c);
        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");
        //运行TensorFlow
        inferenceInterface.run(outputNames, logStats);
        Trace.endSection();

        // Copy the output Tensor back into the output array.
        // 捕捉输出
        Trace.beginSection("fetch");
        //inferenceInterface.fetch(outputNameint, outputint);
        Trace.endSection();
        String log = "\n" + target + ":\n";

        Log.i("TensorflowesturePredict", "result:" + outputint[0]);

    }

    /**
     * 按行读取txt
     *
     * @param is
     * @return
     * @throws Exception
     */
    private ArrayList<Float> readTextFromFile(InputStream is) throws Exception {
        InputStreamReader reader = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuffer buffer = new StringBuffer("");
        ArrayList<Float> data = new ArrayList<>();
        String str;
        while ((str = bufferedReader.readLine()) != null) {
            data.add(Float.parseFloat(str));
        }


        return data;
    }
}
