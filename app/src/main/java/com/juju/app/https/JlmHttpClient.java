package com.juju.app.https;

import android.util.Log;

import com.juju.app.bean.json.BaseReqBean;
import com.juju.app.bean.json.BaseResBean;
import com.juju.app.bean.json.LoginResBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.utils.JacksonUtil;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

/**
 * 项目名称：juju
 * 类描述：基于Xutils框架 http封装类
 * 创建人：gm
 * 日期：2016/2/17 16:50
 * 版本：V1.0.0
 */
public class JlmHttpClient<Req> {

    /**
     * 请求资源ID
     */
    private int accessId;

    /**
     * URL地址
     */
    private String url;


    /**
     * 回调函数，完成Http请求需要实现此接口
     */
    private HttpCallBack callBack;

    /**
     * 请求封装对象，可以为MAP或自定义封装对象
     */
    private Req req;

    /**
     * 响应封装对象，可为空
     */
    private Class res;


    public JlmHttpClient(int accessId, String url,
                         HttpCallBack callBack, Req req) {
        this.accessId = accessId;
        this.req = req;
        this.url = url;
        this.callBack = callBack;
    }

    public JlmHttpClient(int accessId, String url,
            HttpCallBack callBack, Req req, Class resClass) {
        this.accessId = accessId;
        this.url = url;
        this.callBack = callBack;
        this.req = req;
        this.res = resClass;
    }

    /**
     *
     * 方法名： sendGet
     * 方法描述：发送Get请求
     *
     */
    public void sendGet() throws UnsupportedEncodingException, JSONException {
        RequestParams params = new RequestParams();
        initGetRequestParams(req, params);
        doSend(params, HttpRequest.HttpMethod.GET);
    }

    /**
     *
     * 方法名： sendPost
     * 方法描述：发送Post请求
     *
     */
    public void sendPost() throws UnsupportedEncodingException, JSONException {
        RequestParams params = new RequestParams();
        initPostRequestParams(req, params);
        doSend(params, HttpRequest.HttpMethod.POST);
    }

    /**
     *
     * 方法名： sendSyncGet
     * 方法描述：发送Get请求（无响应）
     *
     */
    public void sendSyncGet() throws UnsupportedEncodingException, JSONException, HttpException {
        RequestParams params = new RequestParams();
        initGetRequestParams(req, params);
        doSyncSend(params, HttpRequest.HttpMethod.GET);
    }

    /**
     *
     * 方法名： sendSyncPost
     * 方法描述：发送Post请求（无响应）
     *
     */
    public void sendSyncPost() throws UnsupportedEncodingException, JSONException, HttpException {
        RequestParams params = new RequestParams();
        initGetRequestParams(req, params);
        doSyncSend(params, HttpRequest.HttpMethod.GET);
    }

    /**
     *
     * 方法名： initGetRequestParams
     * 方法描述： 封装GET请求RequestParams
     * 参数说明：valueMap：输入参数 params：RequestParams实例
     * 返回类型： void
     *
     */
    private void initGetRequestParams(Req req, RequestParams params)
            throws JSONException, UnsupportedEncodingException {
        params.addHeader("content-type", HttpConstants.CONTENT_TYPE);
        Map<String, Object> valueMap = null;
        if(req instanceof  Map) {
            valueMap = (Map<String, Object>) req;
        }
        if(valueMap != null && valueMap.size() > 0) {
            Set<Map.Entry<String, Object>> entrySet =  valueMap.entrySet();
            for(Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                Object obj = entry.getValue();
                if(obj instanceof String) {
                    String valueStr = (String) obj;
                    params.addQueryStringParameter(key, valueStr);
                } else if (obj instanceof Integer) {
                    Integer valueInt = (Integer) obj;
                    params.addQueryStringParameter(key, valueInt.toString());
                }
            }
        }
    }

    /**
     *
     * 方法名： initPostRequestParams
     * 方法描述： 封装POST请求RequestParams
     * 参数说明：valueMap：输入参数 params：RequestParams实例
     * 返回类型： void
     *
     */
    private void initPostRequestParams(Req req, RequestParams params)
            throws JSONException, UnsupportedEncodingException {
        params.addHeader("content-type", HttpConstants.CONTENT_TYPE);
        JSONObject paramJson = new JSONObject();
        String body = "";
        Map<String, Object> valueMap = null;
        if(req instanceof  Map) {
            valueMap = (Map<String, Object>) req;
        } else if (req instanceof BaseReqBean) {
            body = JacksonUtil.turnObj2String(req);
        }
        if(valueMap != null && valueMap.size() > 0) {
            Set<Map.Entry<String, Object>> entrySet =  valueMap.entrySet();
            for(Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                Object obj = entry.getValue();
                if(obj instanceof String) {
                    String valueStr = (String) obj;
                    paramJson.put(key, valueStr);
                } else if (obj instanceof Integer) {
                    Integer valueInt = (Integer) obj;
                    paramJson.put(key, valueInt);
                }
            }
            body = paramJson.toString();
            Log.d("["+url+"]Send Post Message:", body);
        }
        params.setBodyEntity(new StringEntity(body));
    }

    private void doSend(RequestParams params, HttpRequest.HttpMethod httpMethod) {
        HttpUtils http = new HttpUtils();
        http.send(httpMethod, url, params,
                new RequestCallBack<String>() {
                    @Override
                    public void onSuccess(ResponseInfo<String> responseInfo) {
                        if (res == null || res.isAssignableFrom(String.class)) {
                            callBack.onSuccess(responseInfo, accessId);
                        } else {
                            callBack.onSuccess(responseInfo, accessId,
                                    JacksonUtil.turnString2Obj(responseInfo.result, res));
                        }
                    }
                    @Override
                    public void onFailure(HttpException error, String msg) {
                        callBack.onFailure(error, msg, accessId);
                    }
                });
    }

    private void doSyncSend(RequestParams params, HttpRequest.HttpMethod httpMethod)
            throws HttpException {
        HttpUtils http = new HttpUtils();
        http.sendSync(httpMethod, url, params);
    }
}
