package com.juju.app.entity.chat;


import android.text.TextUtils;


import com.juju.app.golobal.DBConstant;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.pinyin.PinYinUtil;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


@Table(name = "user_group", onCreated = "CREATE UNIQUE INDEX index_group_peer_id on user_group(peer_id)")
public class GroupEntity extends PeerEntity {


    @Column(name = "group_type")
    private int groupType;

    @Column(name = "creator_id")
    private String creatorId;

    @Column(name = "user_cnt")
    private int userCnt;

    @Column(name = "user_list")
    private String userList;

    @Column(name = "version")
    private int version;

    @Column(name = "status")
    private int status;

    /**
     * 描述
     */
    @Column(name = "desc")
    private String desc;

    //二维码
    @Column(name = "qr_code")
    private String qrCode;

    //邀请码
    @Column(name = "invite_code")
    private String inviteCode;


    private PinYinUtil.PinYinElement pinyinElement = new PinYinUtil.PinYinElement();
    private SearchElement searchElement = new SearchElement();

    public GroupEntity() {

    }

    public GroupEntity(Long localId) {
        this.localId = localId;
    }

//    public GroupEntity(Long localId, String id, String peerId, int groupType, String mainName,
//                       String avatar, String creatorId, int userCnt, String userList,
//                       int version, int status, int created, int updated, String desc) {
//        this.localId = localId;
//        this.id = id;
//        this.peerId = peerId;
//        this.groupType = groupType;
//        this.mainName = mainName;
//        this.avatar = avatar;
//        this.creatorId = creatorId;
//        this.userCnt = userCnt;
//        this.userList = userList;
//        this.version = version;
//        this.status = status;
//        this.created = created;
//        this.updated = updated;
//        this.desc = desc;
//    }



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

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
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

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    @Override
    public int getType() {
        return DBConstant.SESSION_TYPE_GROUP;
    }



    public Set<String> getlistGroupMemberIds(){
        if(TextUtils.isEmpty(userList)){
          return  Collections.emptySet();
        }
        String[] arrayUserIds =  userList.trim().split(",");
        if(arrayUserIds.length <=0){
            return Collections.emptySet();
        }
        /**zhe'g*/
        Set<String> result = new TreeSet<String>();
        for(int index=0;index < arrayUserIds.length;index++){
            String userId =  arrayUserIds[index];
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

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }



//    GroupEntity groupEntity = new GroupEntity(0l, id,
//            peerId, 0, name, avatarSbf.toString(), creatorId,
//            jsonArray.length(), userNoSbf.toString(),
//            0, DBConstant.GROUP_STATUS_ONLINE, 0, 0, desc);

    /**
     * 获取群组信息
     * @param id
     * @param peerId
     * @param groupType
     * @param name
     * @param userNos
     * @param creatorId
     * @param desc
     * @return
     */
    public static GroupEntity buildForReceive(String id, String peerId, int groupType, String name,
                                              String userNos, String creatorId, String desc,
                                              Date created, Date updated) {
        if(StringUtils.isBlank(userNos)
                || StringUtils.isBlank(id)
                || StringUtils.isBlank(peerId))
            throw new IllegalArgumentException("groupEntity#buildForReceive is error");

        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(id);
        groupEntity.setPeerId(peerId);
        groupEntity.setGroupType(groupType);
        groupEntity.setMainName(name);
        String[] userNoArr = userNos.split(",");
        groupEntity.setUserCnt(userNoArr.length);

        StringBuilder userNoSbf = new StringBuilder();
        for (int i = 0; i <userNoArr.length ; i++) {
            String userNo = userNoArr[i];
            userNoSbf.append(userNo);
            if(i < userNoArr.length - 1) {
                userNoSbf.append(",");
            }
        }
        groupEntity.setUserList(userNoSbf.toString());
        groupEntity.setCreatorId(creatorId);
        groupEntity.setDesc(desc);
        if(created != null) {
            groupEntity.setCreated(created.getTime());
        }
        if(updated != null) {
            groupEntity.setUpdated(updated.getTime());
        }
        return groupEntity;
    }

    /**
     * 创建群聊
     * @param id
     * @param peerId
     * @param groupType
     * @param name
     * @param creatorId
     * @param desc
     * @param time
     * @return
     */
    public static GroupEntity buildForCreate(String id, String peerId, int groupType,
                                             String name, String creatorId, String desc, Date time) {
        if(StringUtils.isBlank(id)
                || StringUtils.isBlank(peerId))
            throw new IllegalArgumentException("groupEntity#buildForCreate is error");

        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(id);
        groupEntity.setPeerId(peerId);
        groupEntity.setGroupType(groupType);
        groupEntity.setMainName(name);
        groupEntity.setUserCnt(1);
        groupEntity.setUserList(creatorId);
        groupEntity.setCreatorId(creatorId);
        groupEntity.setDesc(desc);
        if(time != null) {
            groupEntity.setCreated(time.getTime());
        }
        //正常状态（非屏蔽）
        groupEntity.setStatus(DBConstant.GROUP_STATUS_ONLINE);
        return groupEntity;
    }


}
