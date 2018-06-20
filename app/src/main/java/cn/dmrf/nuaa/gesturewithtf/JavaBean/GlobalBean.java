package cn.dmrf.nuaa.gesturewithtf.JavaBean;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.dmrf.gesturewithncnn.R;

import java.io.IOException;
import java.util.ArrayList;

import cn.dmrf.nuaa.gesturewithtf.JniClass.SignalProcess;
import cn.dmrf.nuaa.gesturewithtf.Utils.ClientToServerUtil;
import cn.dmrf.nuaa.gesturewithtf.Utils.ControlSystemActionUtil;
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
    public String[] gesture_name = {"Static", "Approaching", "Aparting", "Click", "Flip", "Circle"};

    public TensorFlowUtil tensorFlowUtil;

    public boolean toServerFlag = false;
    private ControlSystemActionUtil controlSystemActionUtil;


    /*
    views
     */
    public CheckBox debug_checkbox;
    public CheckBox sys_action_checkbox;

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
    private boolean sys_action_flag = false;//启用系统动作标志

    public ArrayList<Double> L_I[];
    public ArrayList<Double> L_Q[];


    private Context context;

    public SignalProcess signalProcess;

    public ClientToServerUtil clientToServerUtil;


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
                } catch (IndexOutOfBoundsException e) {//捕捉一下数组越界的异常，偶尔会出现数据缺失导致数组越界的情况，应该是audio层有问题
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
            tvDist.setText("！" + gesture_name[inde]);
        }


        inde = tensorFlowUtil.PredictContinous(a, 1);
        UpDataUi(inde);


    }


    private void UpDataUi(int label) {
        if (label == -1) {
            tvDist2.setText("...");
        } else {
            tvDist2.setText(gesture_name[label]);
        }

        // {"Static", "Approaching", "Aparting", "Click", "Flip", "Circle"}
        /*
        - approach——screenlighter 1
        - apart——screendarker 2
        - cicle——volumeup 5
        - click——volumedown 3
        - flip——showdesktop 4
         */
        if (sys_action_flag) {
            switch (label) {
                case 0:
                    break;
                case 1:
                    controlSystemActionUtil.lighter();
                    break;
                case 2:
                    controlSystemActionUtil.darker();
                    break;
                case 3:
                    controlSystemActionUtil.volumedown();
                    break;
                case 4:
                    controlSystemActionUtil.showdesk();
                    break;
                case 5:
                    controlSystemActionUtil.volumeup();
                    break;
            }
        }
    }

    public GlobalBean(Context context) {
        this.context = context;
    }

    public void Init() throws IOException {


        clientToServerUtil = new ClientToServerUtil();

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

        debug_checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (debug_checkbox.isChecked()) {
                    toServerFlag = true;
                    if (!clientToServerUtil.isHas_url()) {
                        SetIpDialog();
                    }
                } else {
                    toServerFlag = false;
                }
            }
        });

        sys_action_checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sys_action_checkbox.isChecked()) {
                    sys_action_flag = true;
                    controlSystemActionUtil.InitSys();
                } else {
                    sys_action_flag = false;
                }
            }
        });


    }


    @SuppressLint("ResourceAsColor")
    public void Stop() {
        flag1 = true;
    }

    public void AddDataToList(ArrayList<Double>[] list, double[] data, boolean IsI) {


        int count = -1;
        for (int i = 0; i < 880; i++) {
            if (i % 110 == 0) {
                count++;
            }

            list[count].add(data[i]);
        }

        if (toServerFlag) {

            float send[] = new float[110];

            for (int i = 0; i < 110; i++) {
                send[i] = (float) data[i];

            }
            if (IsI) {
                clientToServerUtil.SendMessageToServer("gesture", send, "senddata_i");
            } else {
                clientToServerUtil.SendMessageToServer("gesture", send, "senddata_q");
            }


        }


    }


    private void StartInit() {

        if (!clientToServerUtil.isHas_url()) {

        }
        if (L_I[0] != null) {
            for (int i = 0; i < 8; i++) {
                L_I[i].clear();
                L_Q[i].clear();
            }
        }
        flag = true;
    }

    private void SetIpDialog() {
        AlertDialog.Builder customizeDialog =
                new AlertDialog.Builder(context);
        final View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_customize, null);
        customizeDialog.setTitle("设置服务器的IP地址");
        customizeDialog.setView(dialogView);
        customizeDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 获取EditView中的输入内容
                        EditText edit_text =
                                (EditText) dialogView.findViewById(R.id.edit_text);
                        String ip = edit_text.getText().toString();
                        clientToServerUtil.setUrl(ip);
                    }
                });
        customizeDialog.show();
    }
}
