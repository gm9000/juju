package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

/**
 * 项目名称：juju
 * 类描述：位置报告事件
 * 创建人：JanzLee
 * 日期：2016/7/13 19:57
 * 版本：V1.0.0
 */
public class LocationReportEvent {

    public LocationReportBean bean;

    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;


    public LocationReportEvent(Event event, LocationReportBean bean) {
        this.bean = bean;
        this.event = event;
    }

    public LocationReportEvent(Event event) {
        this.event = event;
    }



    @JsonIgnoreProperties(value = {"replyId", "replyTime"})
    public static class LocationReportBean extends ReplyBean {
        private String groupId;
        private String partyId;
        private double latitude;
        private double longitude;
        private double distance;
        private String userNo;
        private String nickName;
        private boolean exit = false;

        public static LocationReportBean valueOf(String groupId,String partyId, double latitude,double longitude,
                                                 double distance,String userNo, String nickName) {
            LocationReportBean bean = new LocationReportBean();
            bean.groupId = groupId;
            bean.partyId = partyId;
            bean.latitude = latitude;
            bean.longitude = longitude;
            bean.distance = distance;
            bean.userNo = userNo;
            bean.nickName = nickName;
            return bean;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getPartyId() {
            return partyId;
        }

        public void setPartyId(String partyId) {
            this.partyId = partyId;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public String getUserNo() {
            return userNo;
        }

        public void setUserNo(String userNo) {
            this.userNo = userNo;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public boolean isExit() {
            return exit;
        }

        public void setExit(boolean exit) {
            this.exit = exit;
        }
    }

    public enum Event {
        LOCATION_REPORT_OK,
        LOCATION_REPORT_FAILED
    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public LocationReportBean bean;

            public SendParam(Send send, LocationReportBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {

                //投票发布（消息服务器）
                SEND_LOCATION_REPORT_MSERVER_OK,
                SEND_LOCATION_REPORT_MSERVER_FAILED,

            }
        }

        public static class RecvParam {
            public Recv recv;
            public LocationReportBean bean;


            public RecvParam(Recv recv, LocationReportBean bean) {
                this.recv = recv;
                this.bean = bean;
            }


            public enum Recv {
                //本地数据处理
                PROCESS_LOCAL_CACHE_DATA_OK,
                PROCESS_LOCAL_CACHE_DATA_FAILED,

            }
        }
    }
}
