package cn.dmrf.nuaa.gesturewithtf.Utils;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ClientToServerUtil {
    private String url = "http://" + "112.86.199.135" + ":5000/";
    private String type = "senddata_i";

    private boolean has_url = true;

    public ClientToServerUtil(String url) {
        this.url = "http://" + "112.86.199.151" + ":5000/";

        has_url = true;
    }

//    public void SendMessageToServer(String key, final String value) {
//
//        FormBody.Builder formBuilder = new FormBody.Builder();
//        formBuilder.add(key, value);
//        request = new Request.Builder().url(url).post(formBuilder.build()).build();
//
//        call = client.newCall(request);
//
//
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                //服务器错误
//                Log.e("gesture_send","服务器错误"+url);
//            }
//
//            @Override
//            public void onResponse(Call call, final Response response) throws IOException {
//                final String res = response.body().string();
//                Log.e("gesture_send","成功:"+res);
//            }
//        });
//    }

    public void SendMessageToServer(String key, float[] value,String method) {
        String str_value = FloatArrayToString(value);
        OkHttpClient client = new OkHttpClient();
        FormBody.Builder formBuilder = new FormBody.Builder();
        formBuilder.add(key, str_value);

        Request request = new Request.Builder().url(url+method).post(formBuilder.build()).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String res = response.body().string();

            }
        });

    }

    public void setUrl(String url) {
        this.url = "http://" + url + ":5000/";
        has_url = true;
    }

    public boolean isHas_url() {
        return has_url;
    }

    public ClientToServerUtil() {
        //client = new OkHttpClient();

    }

    private String FloatArrayToString(float[] value) {
        String str_value = "";
        for (int i = 0; i < value.length - 1; i++) {
            str_value = str_value + value[i] + ",";
        }
        str_value += value[value.length - 1];

        return str_value;
    }
}
