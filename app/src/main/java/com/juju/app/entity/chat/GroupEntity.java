package com.juju.app.entity.chat;


import android.text.TextUtils;


import com.juju.app.golobal.DBConstant;
import com.juju.app.utils.pinyin.PinYinUtil;
import com.lidroid.xutils.db.annotation.Column;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class GroupEntity extends PeerEntity {


    @Column(column = "group_type")
    private int groupType;

    @Column(column = "creator_id")
    private int creatorId;

    @Column(column = "user_cnt")
    private int userCnt;

    @Column(column = "user_list")
    private String userList;

    @Column(column = "version")
    private int version;

    @Column(column = "status")
    private int status;


    private PinYinUtil.PinYinElement pinyinElement = new PinYinUtil.PinYinElement();
    private SearchElement searchElement = new SearchElement();

    public GroupEntity() {

    }

    public GroupEntity(Long id) {
        this.id = id;
    }

    public GroupEntity(Long id, String peerId, int groupType, String mainName,
                       String avatar, int creatorId, int userCnt, String userList,
                       int version, int status, int created, int updated) {
        this.id = id;
        this.peerId = peerId;
        this.groupType = groupType;
        this.mainName = mainName;
        this.avatar = avatar;
        this.creatorId = creatorId;
        this.userCnt = userCnt;
        this.userList = userList;
        this.version = version;
        this.status = status;
        this.created = created;
        this.updated = updated;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public int getGroupType() {
        return groupType;
    }

    public void setGroupType(int groupType) {
        this.groupType = groupType;
    }

    public String getMainName() {
        return mainName;
    }

    public void setMainName(String mainName) {
        this.mainName = mainName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public int getUserCnt() {
        return userCnt;
    }

    public void setUserCnt(int userCnt) {
        this.userCnt = userCnt;
    }

    public String getUserList() {
        return userList;
    }

    public void setUserList(String userList) {
        this.userList = userList;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    @Override
    public int getType() {
        return DBConstant.SESSION_TYPE_GROUP;
    }


    public Set<Integer> getlistGroupMemberIds(){
        if(TextUtils.isEmpty(userList)){
          return  Collections.emptySet();
        }
        String[] arrayUserIds =  userList.trim().split(",");
        if(arrayUserIds.length <=0){
            return Collections.emptySet();
        }
        /**zhe'g*/
        Set<Integer> result = new TreeSet<Integer>();
        for(int index=0;index < arrayUserIds.length;index++){
            int userId =  Integer.parseInt(arrayUserIds[index]);
            result.add(userId);
        }
        return result;
    }
    public void setlistGroupMemberIds(List<Integer> memberList){
        String userList = TextUtils.join(",",memberList);
        setUserList(userList);
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
}
