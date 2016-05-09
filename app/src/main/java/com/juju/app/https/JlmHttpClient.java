package com.juju.app.https;

import android.graphics.Bitmap;
import android.util.Log;

import com.juju.app.bean.json.BaseReqBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.view.imagezoom.utils.BitmapUtils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;


import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    private final String TAG = getClass().getName();

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

    private HttpCallBack4OK callBack2;

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


    public JlmHttpClient(int accessId, String url,
                         HttpCallBack4OK callBack2, Req req, Class resClass) {
        this.accessId = accessId;
        this.url = url;
        this.req = req;
        this.res = resClass;
        this.callBack2 = callBack2;
    }


    /**
     *
     * 方法名： sendGet
     * 方法描述：发送Get请求
     *
     */
    @Deprecated
    public void sendGet() throws UnsupportedEncodingException, JSONException {
        RequestParams params = new RequestParams();
        initGetRequestParams(req, params);
        doSend(params, HttpRequest.HttpMethod.GET);
    }

    /**
     *
     * 方法名： sendGet
     * 方法描述：发送Get请求
     *
     */
    @Deprecated
    public void sendGetNoCache() throws UnsupportedEncodingException, JSONException {
        RequestParams params = new RequestParams();
        initGetRequestParams(req, params);
        doSendNoCache(params, HttpRequest.HttpMethod.GET);
    }

    /**
     *
     * 方法名： sendPost
     * 方法描述：发送Post请求
     *
     */
    @Deprecated
    public void sendPost() throws UnsupportedEncodingException, JSONException {
        RequestParams params = new RequestParams();
        initPostRequestParams(req, params);
        doSend(params, HttpRequest.HttpMethod.POST);
    }

    /**
     *
     * 方法名： sendPost
     * 方法描述：发送Post请求
     *
     */
    @Deprecated
    public void sendUpload() throws UnsupportedEncodingException, JSONException {
        RequestParams params = new RequestParams();
        initPostUploadParams(req, params);
        doSend(params, HttpRequest.HttpMethod.POST);
    }



    /**
     *
     * 方法名： sendGet4OK
     * 方法描述：发送Get请求
     *
     */
    public void sendGet4OK() throws UnsupportedEncodingException, JSONException {
        final Request.Builder build = new Request.Builder();
        initGetRequestBuilder(req, build);
        final Request request = build.build();
        doSend(request);
    }

    /**
     *
     * 方法名： sendGet
     * 方法描述：发送Get请求
     *
     */
    public void sendGetNoCache4OK() throws UnsupportedEncodingException, JSONException {
        final Request.Builder build = new Request.Builder();
        build.cacheControl(CacheControl.FORCE_NETWORK);
        initGetRequestBuilder(req, build);
        final Request request = build.build();
        doSend(request);
    }

    /**
     *
     * 方法名： sendPost
     * 方法描述：发送Post请求
     *
     */
    public void sendPost4OK() throws UnsupportedEncodingException, JSONException {
        final Request.Builder build = new Request.Builder();
        initPostRequestBuilder(req, build);
        final Request request = build.build();
        doSend(request);
    }

    /**
     *
     * 方法名： sendPost
     * 方法描述：发送Post请求
     *
     */
    public void sendUpload4OK() throws UnsupportedEncodingException, JSONException {
        final Request.Builder build = new Request.Builder();
        initPostUploadBuilder(req, build);
        final Request request = build.build();
        doSend(request);
    }



    /**
     *
     * 方法名： initGetRequestBuilder
     * 方法描述： 封装GET请求RequestParams
     * 参数说明：valueMap：输入参数 params：RequestParams实例
     * 返回类型： void
     *
     */
    private void initGetRequestBuilder(Req req, Request.Builder build)
            throws JSONException, UnsupportedEncodingException {
        Map<String, Object> valueMap = null;
        if(req instanceof  Map) {
            valueMap = (Map<String, Object>) req;
        }
        String parameter = "";
        if(valueMap != null && valueMap.size() > 0) {
            parameter = getUrlParamsByMap(valueMap);
        }
        build
                .url(url+parameter)
                .header("Content-type", HttpConstants.CONTENT_TYPE)
                .header("Connection", HttpConstants.CONNECTION_CLOSE)
                .get();
    }


    /**
     *
     * 方法名： initPostRequestParams
     * 方法描述： 封装POST请求RequestParams
     * 参数说明：valueMap：输入参数 params：RequestParams实例
     * 返回类型： void
     *
     */
    private void initPostUploadBuilder(Req req, Request.Builder build)
            throws JSONException, UnsupportedEncodingException {
        Map<String, Object> valueMap = (Map<String, Object>) req;
        if(valueMap != null && valueMap.size() > 0) {
            Set<Map.Entry<String, Object>> entrySet =  valueMap.entrySet();
            MultipartBuilder multipartBuilder = new MultipartBuilder().type(MultipartBuilder.FORM);
            for(Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                Object obj = entry.getValue();
                if(obj instanceof String) {
                    String valueStr = (String) obj;
                    multipartBuilder.addFormDataPart(key, valueStr);
                } else if (obj instanceof Integer) {
                    Integer valueInt = (Integer) obj;
                    multipartBuilder.addFormDataPart(key, valueInt.toString());
                }else if(obj instanceof Bitmap){
                    Object[] inputArray = BitmapUtils.bitmap2InputStream2((Bitmap) obj);
                    int size = (int)inputArray[0];
                    RequestBody fileBodySmall = RequestBody.create(
                            MediaType.parse("application/octet-stream"), (byte[])inputArray[1], 0, size);

                    //根据文件名设置contentType
                    multipartBuilder.addPart(Headers.of("Content-Disposition", "form-data; " +
                            "name=\""+key+"\"; filename=\""+key+"\""), fileBodySmall);

                    multipartBuilder.addPart(fileBodySmall);
                }else if(obj instanceof File){
                    RequestBody fileBody = RequestBody.create(
                            MediaType.parse("application/octet-stream"), (File)obj);

                    multipartBuilder.addPart(Headers.of("Content-Disposition", "form-data; " +
                            "name=\"" + key + "\"; filename=\"" + key + "\""), fileBody);
                }
                RequestBody requestBody = multipartBuilder.build();
                build
                        .header("Connection", HttpConstants.CONNECTION_CLOSE)
                        .url(url)
                        .post(requestBody);
            }
        }
    }


    private void initPostRequestBuilder(Req req, Request.Builder build)
            throws JSONException, UnsupportedEncodingException {
        String body = "";
        Map<String, Object> valueMap = null;
        if(req instanceof  Map) {
            valueMap = (Map<String, Object>) req;
        } else if (req instanceof BaseReqBean) {
            body = JacksonUtil.turnObj2String(req);
        }
        if(valueMap != null && valueMap.size() > 0) {
            JSONObject paramJson = getJSONObject(valueMap);
            body = paramJson.toString();
        }
        Log.d("[" + url + "]Send Post Message:", body);
        build
                .url(url)
                .header("Connection", HttpConstants.CONNECTION_CLOSE)
                .post(RequestBody.create(MediaType.parse(HttpConstants.CONTENT_TYPE), body));

    }



    private void doSend(Request request) {
        final Call call = OkHttpClientManager.getInstance().getmOkHttpClient().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                callBack2.onFailure(e, accessId);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (res == null || res.isAssignableFrom(String.class)) {
                    callBack2.onSuccess(response, accessId);
                } else if (res.isAssignableFrom(JSONObject.class)) {
                    try {
                        callBack2.onSuccess(new JSONObject(response.body().string()), accessId);
                    } catch (JSONException e) {
                        Log.e(TAG, "转换JSONObject对象失败", e);
                        callBack2.onFailure(new IOException("转换JSONObject对象失败"), accessId);
                    }
                } else {
                    callBack2.onSuccess(
                            JacksonUtil.turnString2Obj(response.body().string(), res), accessId);
                }

            }
        });
    }



    private void doSyncSend(RequestParams params, HttpRequest.HttpMethod httpMethod)
            throws HttpException {
        HttpUtils http = new HttpUtils();
        http.sendSync(httpMethod, url, params);
    }

    private JSONObject getJSONObject(Map valueMap) throws JSONException {
        JSONObject paramJson = new JSONObject();
        Set<Map.Entry> entrySet = valueMap.entrySet();
        for (Map.Entry entry : entrySet) {
            String key = (String) entry.getKey();
            Object obj = entry.getValue();
            if(obj instanceof String) {
                String valueStr = (String) obj;
                paramJson.put(key, valueStr);
            } else if (obj instanceof Integer) {
                Integer valueInt = (Integer) obj;
                paramJson.put(key, valueInt);
            } else if (obj instanceof Map) {
                Map map = (Map)obj;
                JSONObject childJsonObj = new JSONObject(map);
                paramJson.put(key, childJsonObj);
            }
        }
        return paramJson;
    }

    private String getUrlParamsByMap(Map<String, Object> map) {
        if (map == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer("?");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue());
            sb.append("&");
        }
        String s = sb.toString();
        if (s.endsWith("&")) {
            s = org.apache.commons.lang.StringUtils.substringBeforeLast(s, "&");
        }
        return s;
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
    private void initPostUploadParams(Req req, RequestParams params)
            throws JSONException, UnsupportedEncodingException {
//        params.addHeader("content-type", HttpConstants.UPLOAD_TYPE);
        JSONObject paramJson = new JSONObject();
        Map<String, Object> valueMap = (Map<String, Object>) req;
        if(valueMap != null && valueMap.size() > 0) {
            Set<Map.Entry<String, Object>> entrySet =  valueMap.entrySet();
            for(Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                Object obj = entry.getValue();
                if(obj instanceof String) {
                    String valueStr = (String) obj;
                    params.addBodyParameter(key,valueStr);
                } else if (obj instanceof Integer) {
                    Integer valueInt = (Integer) obj;
                    params.addBodyParameter(key, valueInt.toString());
                }else if(obj instanceof Bitmap){
                    Object[] inputArray = BitmapUtils.bitmap2InputStream((Bitmap) obj);
                    long size = (int)inputArray[0];
                    params.addBodyParameter(key,(InputStream)inputArray[1], size);
                }else if(obj instanceof File){
                    params.addBodyParameter(key,(File)obj);
                }
            }
        }
    }


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
                        } else if (res.isAssignableFrom(JSONObject.class)) {
                            try {
                                callBack.onSuccess(responseInfo, accessId,
                                        new JSONObject(responseInfo.result));
                            } catch (JSONException e) {
                                Log.e(TAG, "转换JSONObject对象失败", e);
                                callBack.onSuccess(responseInfo, accessId);
                            }
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

    private void doSendNoCache(RequestParams params, HttpRequest.HttpMethod httpMethod) {
        HttpUtils http = new HttpUtils();
        http.configCurrentHttpCacheExpiry(0);
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
}
