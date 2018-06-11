package cn.dmrf.nuaa.gesturewithtf.Utils;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TensorFlowUtil {

    private TensorFlowInferenceInterface inferenceInterface;

    private String input_cnn_name = "input";
    private String fullconnection1_name = "fullconnection1";
    private String input_lstm_name = "input_lstm";
    private String output_lstm_name = "output_lstm";
    private String cnn_accuracy_name = "softmax";
    private String lstm_accuracy_name = "softmax_lstm";
    //存储cnn和lstm的可信度
    private float[] cnn_softmax;//大小为13
    private float[] lstm_softmax;//大小为6

    private String[] outputNames;
    private String[] outputNames2;
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
            outputNames = new String[]{fullconnection1_name};
            //如果需要cnn部分的置信度
            //outputNames = new String[]{fullconnection1，cnn_accuracy};
            //outputNames2 = new String[]{output_lstm};
            //如果需要lstm部分的置信度
            outputNames2 = new String[]{output_lstm_name, lstm_accuracy_name};


            outputs = new float[classes];
            outputfuuconnection = new float[256];

            outputint = new long[1];
            cnn_softmax = new float[13];
            lstm_softmax = new float[6];


        } catch (Exception e) {
            System.out.println("模型加载失败");
        }
    }

    @SuppressLint("LongLogTag")
    public long PredictContinousTest() {
        float test[] = new float[8800];

        outputint[0] = -1;
        inferenceInterface.feed(input_cnn_name, test, batch, 8, 550, 2);

        inferenceInterface.run(outputNames, logStats);

        Trace.beginSection("fetch");
        inferenceInterface.fetch(fullconnection1_name, outputfuuconnection);
        Log.i("TensorflowesturePredict", "result:" + outputint[0]);
        return outputint[0];
    }


    private float zero[] = new float[256];//zero
    List<float[]> pre_256 = new ArrayList<float[]>();//记录最多3个mic fc

    List<float[]> memory_256 = new ArrayList<float[]>();//记录最多4个mic fc


    /*
    不管哪种输入方案Predict的输入始终控制为当前0.5mic的数据，这样可以保证每0.5s出一个label
     */
    //cur_mic是8800的数组
    public int PredictContinous(float cur_mic[], int index_case) {
        float cur_fc[] = FromCnnGetFc(cur_mic);//拿到当前0.5s输入的fc层输出
        float input_lstm[];


        switch (index_case) {
            case 0:
                /*
                第一种解决方案应该取三种输入方案的最高值：
                     + cur 0.5s and 1.5s zeros
                     + one pre 0.5s and cur 0.5s and 1s zeros
                     + two pre 0.5s and cur 0.5s and 0.5s zeros//因为不存在持续时长为1.5s的手势，所以没有这个case
                     + three pre 0.5s and cur 0.5s
                 */


                List<float[]> four_256 = new ArrayList<float[]>();

                int pre256_size = pre_256.size();

                int max_case = -1;
                float max_acc = -1;

                //1
                four_256.add(cur_fc);
                four_256.add(zero);
                four_256.add(zero);
                four_256.add(zero);

                input_lstm = ListToArray(four_256);

                float res0[];
                res0 = PredictContinous0(input_lstm);//输入为1024的长度

                if (res0 != null) {
                    if (res0[1] > max_acc) {
                        max_acc = res0[1];
                        max_case = (int) res0[0];
                    }
                }


                //2

                float res1[];
                if (pre256_size >= 1) {
                    four_256.add(0, pre_256.get(pre256_size - 1));//把最近的前一个256放到第一个位置
                    four_256.remove(4);//把多余的256个0移除
                    input_lstm = ListToArray(four_256);
                    res1 = PredictContinous0(input_lstm);

                    if (res1 != null) {
                        if (res1[1] > max_acc) {
                            max_acc = res1[1];
                            max_case = (int) res1[0];
                        }
                    }

                }

                //3

                float res2[];

                 //first_pre_256-cur_256-zero-zero——>third_pre_256-second_pre_256-first_pre_256-cur_256
                if (pre256_size >= 3) {
                    //移除最后两个256的zero
                    four_256.remove(2);
                    four_256.remove(2);

                    //将两个pre 256插入到four_256的前两个位置
                    four_256.add(0, four_256.get(pre256_size - 3));
                    four_256.add(0, four_256.get(pre256_size - 2));

                    input_lstm = ListToArray(four_256);
                    res2 = PredictContinous0(input_lstm);

                    if (res2 != null) {
                        if (res2[1] > max_acc) {
                            max_acc = res2[1];
                            max_case = (int) res2[0];
                        }
                    }

                }

                //将当前256存下来
                pre_256.add(cur_fc);
                if (pre256_size == 4) {//控制划窗的max size
                    pre_256.remove(0);
                }


                return max_case;


            case 1:
                /*
                第二种解决方案只有一种输入方案
                 */

                memory_256.add(cur_fc);
                if (memory_256.size() == 5) {
                    memory_256.remove(0);
                }

                input_lstm = new float[1024];
                for (int i = 0; i < memory_256.size(); i++) {
                    for (int j = 0; j < 256; j++) {
                        input_lstm[i * 256 + j] = memory_256.get(i)[j];
                    }
                }

                int res = PredictContinous1(input_lstm);
                if (res!=-1){//如果返回的label正常则应该清空memory
                    memory_256.clear();
                }
                return res;

        }

        return -1;
    }



    private float[] PredictContinous0(float[] input_lstm) {

        inferenceInterface.feed(input_lstm_name, input_lstm, 1, 1024, 1, 1);
        inferenceInterface.run(outputNames2, logStats);
        //执行下面这一句之后拿到的识别lstm的可信度，lstm_softmax六个位置存储6个label的概率
        inferenceInterface.fetch(lstm_accuracy_name, lstm_softmax);
        float res[] = new float[2];

        int max_index = 0;
        for (int i = 1; i < lstm_softmax.length; i++) {
            if (lstm_softmax[i] > lstm_softmax[max_index]) {
                max_index = i;
            }
        }

        res[0] = max_index;
        res[1] = lstm_softmax[max_index];

        return res;
    }


    private int PredictContinous1(float[] input_lstm) {
        inferenceInterface.feed(input_lstm_name, input_lstm, 1, 1024, 1, 1);
        inferenceInterface.run(outputNames2, logStats);
        //执行下面这一句之后拿到的识别lstm的可信度，lstm_softmax六个位置存储6个label的概率
        inferenceInterface.fetch(lstm_accuracy_name, lstm_softmax);
        inferenceInterface.fetch(output_lstm_name, outputint);
        int max_index = (int) outputint[0];
        if (lstm_softmax[max_index] > 0.9) {
            return max_index;
        }
        return -1;

    }

    //输入是8800的mic data，返回256的全连接层输出
    private float[] FromCnnGetFc(float[] input_cnn) {
        float output_fc[] = new float[256];
        inferenceInterface.feed(input_cnn_name, input_cnn, 1, 8, 550, 2);
        inferenceInterface.run(outputNames, logStats);
        inferenceInterface.fetch(fullconnection1_name, output_fc);
        return output_fc;
    }

    private float[] ListToArray(List<float[]> four_256) {
        float array[] = new float[1024];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 256; j++) {
                array[i * 256 + j] = four_256.get(i)[j];
            }
        }

        return array;
    }

}
