package com.juju.app.golobal;

import android.os.Environment;

/**
 * 项目名称：juju
 * 类描述：系统通用常量
 * 创建人：gm
 * 日期：2016/2/16 16:00
 * 版本：V1.0.0
 */
public class Constants {

    /**
     ******************************************* 参数设置信息******************************************
     */


    // 应用名称
    public static String APP_NAME = "";

    public static final String SHARED_PREFERENCE_NAME = "ele_prefs";

    // SDCard路径
    public static final String SD_PATH = Environment
            .getExternalStorageDirectory().getAbsolutePath();

    // 图片存储路径
    public static final String BASE_PATH = SD_PATH + "/juju/";


    // 缓存图片路径
    public static final String BASE_IMAGE_CACHE = BASE_PATH + "cache/images/";

    //是否是开发模式
    public static final boolean IS_APP_MODEL = true;

    /**
     ******************************************* 参数设置信息结束 ******************************************
     */

    /**
     ******************************************* 群聊参数设置信息******************************************
     */
    public static final String TYPE = "TYPE";
    public static final int MESSAGE_STATE_UNLOAD = 0X0000;
    public static final int MESSAGE_STATE_LOADDING = 0X0001;
    public static final int MESSAGE_STATE_FINISH_SUCCESSED = 0X0002;
    public static final int MESSAGE_STATE_FINISH_FAILED = 0X0003;
    public static final int STOP_PLAY_VOICE = 0X0004;
    public static final int pageSize = 21;
    public static final int yayaPageSize = 8;
    public static final String SESSION_ID_KEY = "session_id";
    public static final String GROUP_ID_KEY = "group_id";
    public static final String KEY_PEERID = "key_peerid";
    public static final String KEY_AVATAR_URL = "key_avatar_url";
    public static final String KEY_IS_IMAGE_CONTACT_AVATAR = "is_image_contact_avatar";


    public static final String SINGLE_CHECK_LIST_DATA = "single_check_list_data";
    public static final String SINGLE_CHECK_TARGET_ID = "single_check_target_id";


    /**
     *1. 配置的全局key
     * */
    public static final String SETTING_GLOBAL = "Global";
    public static final int FRAME_RATE = 20;
    public static final int KEY_FRAME_INTERVAL = 1;


    /**
     * 消息状态
     */
    public static final int UPLOAD_FAILED = 0X0005;
    public static final int UPLOAD_SUCCESSED = 0X0006;

    /**
     * 消息类型
     */
    public static final int DISPLAY_TYPE_TEXT = 0X0007;
    public static final int DISPLAY_TYPE_AUDIO = 0X0008;
    public static final int DISPLAY_TYPE_IMAGE = 0X0009;
    public static final String MSG_OVERVIEW_DISPLAY_TYPE_AUDIO = "[ 语音 ]";
    public static final String MSG_OVERVIEW_DISPLAY_TYPE_IMAGE = "[ 图片 ]";
    public static final String MSG_OVERVIEW_DISPLAY_TYPE_OTHERS = "[ 其它消息 ]";


    // 消息是否已读
    public static final int MESSAGE_UNREAD = 0X0000; // 消息未读
    public static final int MESSAGE_ALREADY_READ = 0X0001; // 消息已读
    public static final int MESSAGE_DISPLAYED = 0X0002;// 消息已展现

    public static final byte MESSAGE_TYPE_TELETEXT = 1; // 消息类型 1:// 图文消息（文本或图片）；

    public static final int POPUP_MENU_TYPE_TEXT = 1;
    public static final int POPUP_MENU_TYPE_IMAGE = 2;
    public static final int POPUP_MENU_TYPE_AUDIO = 3;

    public final static int REQUEST_CODE = 0;


    /**头像尺寸大小定义*/
    public static final String AVATAR_APPEND_32 ="_32x32.jpg";


    // 读取磁盘上文件， 分支判断其类型
    public static final int FILE_SAVE_TYPE_IMAGE = 0X00013;
    public static final int FILE_SAVE_TYPE_AUDIO = 0X00014;


    public static final float MAX_SOUND_RECORD_TIME = 60.0f;// 单位秒
    public static final int MAX_SELECT_IMAGE_COUNT = 6;



    /**
     ******************************************* 注册设置信息******************************************
     */
    //SMSSDK
    public static final String APPKEY = "10393216186c8";
    public static final String APPSECRET = "692210dce5d5a712d1b4416467664471";

    public static final String PHONE = "phone";
    public static final String NICKNAME = "nickName";
    public static final String PASSWORD = "password";

    public static final String REMEMBER_PWD = "rememberPwd";    //  是否记住密码
    public static final String AUTO_LOGIN = "autoLogin";        //  是否自动登录

    public static final String USER_INFO = "userInfo";        //  是否自动登录

    public static final String PROPERTY_TYPE = "propertyType";  //  属性类型
    public static final String PROPERTY_VALUE = "propertyValue";  //  属性名称

    public static final String GROUP_ID = "groupId";    //  群组ID
    public static final String PARTY_ID = "partyId";    //  聚会ID
    public static final String IS_OWNER = "isOwner";    //  是否是创建者
    public static final String PLAN_ID = "planId";    //  聚会ID
    public static final String USER_NO = "userNo";    //  聚会ID
    public static final String LATITUDE = "lantitude";    //  聚会ID
    public static final String LONGITUDE = "longitude";    //  聚会ID
    public static final String ADDRESS = "address";    //  聚会ID
    public static final String LOCATION = "location";    //  我的位置
    public static final String EDIT_MODE = "edit_mode";    //  修改模式

    //message 每次拉取的条数
    public static final int MSG_CNT_PER_PAGE = 15;
    /**
     * event 优先级
     * */
    public static final int SERVICE_EVENTBUS_PRIORITY = 10;
    public static final int MESSAGE_EVENTBUS_PRIORITY = 100;

}
