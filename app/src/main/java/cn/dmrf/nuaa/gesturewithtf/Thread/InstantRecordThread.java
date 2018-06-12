package cn.dmrf.nuaa.gesturewithtf.Thread;

import android.content.Context;
import android.os.Message;

import cn.dmrf.nuaa.gesturewithtf.JavaBean.GlobalBean;
import cn.dmrf.nuaa.gesturewithtf.JniClass.SignalProcess;


/**
 * 即时录音线程
 * Created by dmrf on 18-3-15.
 */

public class InstantRecordThread extends Thread {
    private GlobalBean globalBean;
    private Context context;

    public InstantRecordThread(GlobalBean globalBean, Context context) {
        this.globalBean = globalBean;
        this.context = context;
    }

    @Override
    public void run() {
        short[] bsRecord = new short[globalBean.recBufSize];//recBufSize=4400

        double totPhase = 0;
        double NowPhase = 0;
        //--------------jni------------------------
        globalBean.signalProcess = new SignalProcess();
        globalBean.signalProcess.DemoNew();

        while (!globalBean.flag) {
        }
        try {
            globalBean.audioRecord.startRecording();

        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        }
        int while_count = 0;
        int begin_while = 0;

        while (globalBean.flag)//大循环
        {


            int Len = globalBean.audioRecord.read(bsRecord, 0, globalBean.recBufSize);


            double[] di = new double[110];
            double[] tempIIL = new double[880];
            double[] tempQQL = new double[880];


            globalBean.signalProcess.DemoL(bsRecord, di, tempIIL, tempQQL);


            while_count++;

            if (begin_while > 0) {
                globalBean.AddDataToList(globalBean.L_I, tempIIL,true);
                globalBean.AddDataToList(globalBean.L_Q, tempQQL,false);
            }


            if (while_count == 5 && begin_while == 0) {//跳过开始的5次循环，因为前5个的数据都为0
                Message msg3 = new Message();
                msg3.what = 0;
                msg3.obj = ("start");
                globalBean.mHandler.sendMessage(msg3);//第5次循环之后发送消息开始更新UI
                while_count = 0;
                begin_while++;
            } else if (while_count == 5 && begin_while > 0) {

                while_count = 0;
                begin_while++;
                Message msg2 = new Message();
                msg2.what = 0;
                msg2.obj = ("predict");
                globalBean.mHandler.sendMessage(msg2);


            }
            if (globalBean.flag1) {
                Message msg2 = new Message();
                msg2.what = 0;
                msg2.obj = "stop";
                globalBean.mHandler.sendMessage(msg2);
                break;
            }


            double[] tempIIR = new double[880];
            double[] tempQQR = new double[880];
            globalBean.signalProcess.DemoR(bsRecord, di, tempIIR, tempQQR);


            NowPhase += totPhase / 2;
            while (NowPhase < 0) NowPhase += Math.PI * 2;
            while (NowPhase > Math.PI * 2) NowPhase -= Math.PI * 2;

        }
    }
}
