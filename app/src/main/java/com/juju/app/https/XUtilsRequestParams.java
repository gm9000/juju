package com.juju.app.https;

import org.xutils.http.RequestParams;
import org.xutils.http.annotation.HttpRequest;

/**
 * 项目名称：juju
 * 类描述：Xutils3Q请求参数
 * 创建人：gm
 * 日期：2016/5/12 16:41
 * 版本：V1.0.0
 */
@HttpRequest(path = "")
public class XUtilsRequestParams extends RequestParams {

    public XUtilsRequestParams(String uri) {
        super(uri);
    }
}
