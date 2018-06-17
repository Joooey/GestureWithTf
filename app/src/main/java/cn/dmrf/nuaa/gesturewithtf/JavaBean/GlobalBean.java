package cn.dmrf.nuaa.gesturewithtf.JavaBean;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.util.ArrayList;

import cn.dmrf.nuaa.gesturewithtf.JniClass.SignalProcess;
import cn.dmrf.nuaa.gesturewithtf.Utils.TensorFlowUtil;
import cn.dmrf.nuaa.gesturewithtf.Thread.InstantPlayThread;
import cn.dmrf.nuaa.gesturewithtf.Thread.InstantRecordThread;
import cn.dmrf.nuaa.gesturewithtf.Utils.FrequencyPlayerUtils;


/**
 * Created by dmrf on 18-3-15.
 */

public class GlobalBean {

   /*
   set audio
    */

    public double[] Freqarrary = {17500, 17850, 18200, 18550, 18900, 19250, 19600, 19950, 20300, 20650};        //设置播放频率
    public int encodingBitrate = AudioFormat.ENCODING_PCM_16BIT;// 编码率（默认ENCODING_PCM_16BIT）
    public int channelConfig = AudioFormat.CHANNEL_IN_MONO;        //声道（默认单声道） 单道  MONO单声道，STEREO立体声
    public AudioRecord audioRecord;    //录音对象
    public FrequencyPlayerUtils FPlay;
    public int sampleRateInHz = 44100;//采样率（默认44100，每秒44100个点）
    public int recBufSize = 4400;            //定义录音片长度
    public int numfre = 8;
    public String[] gesture_name = {"Static", "Push Left", "Push Right", "Click", "Flip", "Circle"};

    public TensorFlowUtil tensorFlowUtil;


    /*
    views
     */
    public Button btnPlayRecord;        //开始按钮
    public Button btnStopRecord;        //结束按钮
    public TextView tvDist;

    public TextView tvDist2;

    public ImageView flag_small;


    /*
    variable
     */
    public boolean flag = true;        //播放标志
    public boolean flag1 = false;        //结束标志

    public ArrayList<Double> L_I[];
    public ArrayList<Double> L_Q[];


    private Context context;

    public SignalProcess signalProcess;


    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (msg.obj.toString().equals("predict")) {
                        flag_small.setVisibility(View.VISIBLE);

                        PredictContinousGesture();


                        for (int i = 0; i < 8; i++) {//clear掉数据源，防止影响后面的数据
                            L_I[i].clear();
                            L_Q[i].clear();
                        }

                    } else if (msg.obj.toString().equals("start")) {

                        flag_small.setVisibility(View.VISIBLE);
                        StartInit();
                    } else if (msg.obj.toString().equals("stop")) {
                        flag_small.setVisibility(View.GONE);
                        FPlay.colseWaveZ();
                        audioRecord.stop();
                        flag1 = false;

                    } else if (msg.obj.toString().equals("playe")) {
                        Toast.makeText(context, "发生了异常，请联系最帅的人优化代码～", Toast.LENGTH_SHORT).show();
                    }
                    break;

            }
        }
    };


    /*
    每0.5s执行一次
     */
    private void PredictContinousGesture() {

        float id[] = new float[4400];
        float qd[] = new float[4400];
        int ks = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 550; j++) {
                try {
                    id[ks] = L_I[i].get(j).floatValue();
                    qd[ks] = L_Q[i].get(j).floatValue();
                }catch (IndexOutOfBoundsException e){//捕捉一下数组越界的异常，偶尔会出现数据缺失导致数组越界的情况，应该是audio层有问题
                    return;
                }

                ks++;
            }
        }

        signalProcess.Normalize(id, qd);//归一化


        float dataraw[][][] = new float[8][550][2];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 550; j++) {
                for (int k = 0; k < 2; k++) {
                    if (k == 0) {
                        dataraw[i][j][k] = id[i * 550 + j];
                    } else {
                        dataraw[i][j][k] = qd[i * 550 + j];
                    }

                }
            }
        }

        float a[] = new float[8800];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 550; j++) {
                for (int k = 0; k < 2; k++) {
                    a[k + j * 2 + i * 1100] = dataraw[i][j][k];

                }
            }
        }
        int inde = -1;

        /*
        两种预测方案，index对应0和1
         */
        inde = tensorFlowUtil.PredictContinous(a, 0);
        if (inde == -1) {
            tvDist.setText("...");
        } else {
            tvDist.setText("！"+gesture_name[inde]);
        }


        inde = tensorFlowUtil.PredictContinous(a, 1);
        if (inde == -1) {
            tvDist2.setText("...");
        } else {
            tvDist2.setText(gesture_name[inde]);
        }


    }


    public GlobalBean(Context context) {
        this.context = context;
    }

    public void Init() throws IOException {


        L_I = new ArrayList[8];
        L_Q = new ArrayList[8];

        for (int i = 0; i < 8; i++) {
            ArrayList<Double> list1 = new ArrayList<Double>();
            ArrayList<Double> list2 = new ArrayList<Double>();
            L_I[i] = list1;
            L_Q[i] = list2;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,//从麦克风采集音频
                sampleRateInHz,//采样率，这里的值是sampleRateInHz = 44100即每秒钟采样44100次
                channelConfig,//声道设置，MONO单声道，STEREO立体声，这里用的是立体声
                encodingBitrate,//编码率（默认ENCODING_PCM_16BIT）
                recBufSize);//录音片段的长度，给的是minBufSize=recBufSize = 4400 * 2;


        InitListener();


    }

    private void InitListener() {


        btnPlayRecord.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View v) {

                StartInit();


                new InstantPlayThread(GlobalBean.this).start();        //播放(发射超声波)


                try {
                    Thread.sleep(10);    //等待开始播放再录音
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                new InstantRecordThread(GlobalBean.this, context).start();        //录音
                //录音播放线程

            }
        });


        //停止按钮
        btnStopRecord.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View v) {
                Stop();
                // TODO 自动生成的方法存根

            }
        });


    }


    @SuppressLint("ResourceAsColor")
    public void Stop() {
        flag1 = true;
    }

    public void AddDataToList(ArrayList<Double>[] list, double[] data) {

        int count = -1;
        for (int i = 0; i < 880; i++) {
            if (i % 110 == 0) {
                count++;
            }
            list[count].add(data[i]);
        }
    }


    private void StartInit() {

        if (L_I[0] != null) {
            for (int i = 0; i < 8; i++) {
                L_I[i].clear();
                L_Q[i].clear();
            }
        }
        flag = true;
    }
}
