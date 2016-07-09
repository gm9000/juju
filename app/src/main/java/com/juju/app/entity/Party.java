package com.juju.app.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.juju.app.entity.base.BaseEntity;
import com.juju.app.entity.chat.SearchElement;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.utils.json.JsonDateDeserializer;
import com.juju.app.utils.json.JsonDateSerializer;
import com.juju.app.utils.pinyin.PinYinUtil;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;
import org.xutils.ex.DbException;

import java.util.Date;


@Table(name = "party")
public class Party extends BaseEntity {

    @Column(name = "name")
    private String name;
    @Column(name = "desc")
    private String desc;
    @Column(name = "time")
    private Date time;

    //  -1：草稿箱  0：召集中   1：进行中   2：已结束
    @Column(name = "status")
    private int status;

    //  -1：归档 0：正常 1：特别关注
    @Column(name = "follow_flag")
    private int followFlag;

    @Column(name = "attend_flag")
    private int attendFlag;

    @Column(name = "coverUrl")
    private String coverUrl;

    private boolean descMatch;

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    private boolean hidden;

//    @Foreign(column = "createUserNo",foreign = "userNo")
//    private User creator;

    @Column(name = "user_no")
    private String userNo;

//    @Foreign(column = "groupId",foreign = "id")
//    private GroupEntity group;

    @Column(name = "group_id")
    private String groupId;

    private User creator;

    private PinYinUtil.PinYinElement pinyinElement = new PinYinUtil.PinYinElement();
    private SearchElement searchElement = new SearchElement();


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @JsonSerialize(using=JsonDateSerializer.class)
    public Date getTime() {
        return time;
    }
    @JsonDeserialize(using=JsonDateDeserializer.class)
    public void setTime(Date time) {
        this.time = time;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getFollowFlag() {
        return followFlag;
    }

    public void setFollowFlag(int followFlag) {
        this.followFlag = followFlag;
    }

    public int getAttendFlag() {
        return attendFlag;
    }

    public void setAttendFlag(int attendFlag) {
        this.attendFlag = attendFlag;
    }

    public PinYinUtil.PinYinElement getPinyinElement() {
        return pinyinElement;
    }

    public void setPinyinElement(PinYinUtil.PinYinElement pinyinElement) {
        this.pinyinElement = pinyinElement;
    }

    public SearchElement getSearchElement() {
        return searchElement;
    }

    public void setSearchElement(SearchElement searchElement) {
        this.searchElement = searchElement;
    }

    public boolean isDescMatch() {
        return descMatch;
    }

    public void setDescMatch(boolean descMatch) {
        this.descMatch = descMatch;
    }

    //    public User getCreator() {
//        return creator;
//    }
//
//    public void setCreator(User creator) {
//        this.creator = creator;
//    }
//
//    public GroupEntity getGroup() {
//        return group;
//    }
//
//    public void setGroup(GroupEntity group) {
//        this.group = group;
//    }

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public User getCreator() {
        if(creator == null) {
            try {
                creator = JujuDbUtils.getInstance().selector(User.class).where("user_no", "=", userNo).findFirst();
            } catch (DbException e) {
                e.printStackTrace();
            }
        }
        return creator;
    }

    public void setCoverUrl(String coverUrl){
        this.coverUrl = coverUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

}
