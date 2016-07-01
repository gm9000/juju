package com.juju.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.service.im.manager.IMContactManager;
import com.juju.app.utils.Logger;
import com.juju.app.view.groupchat.IMGroupAvatar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class GroupListAdapter extends BaseAdapter  implements AdapterView.OnItemClickListener{
    private Logger logger = Logger.getLogger(GroupListAdapter.class);

    private List<GroupEntity>  groupList = new ArrayList<GroupEntity>();
    private List<GroupEntity>  matchGroupList = new ArrayList<GroupEntity>();
    private Map<String,GroupHolder> holderMap = new HashMap<String,GroupHolder>();

    private String currentGroupId;

    private boolean isSearchMode= false;
    private String searchKey;
    private Context ctx;

    public GroupListAdapter(Context ctx){
        this.ctx = ctx;
    }

    public void setGroupList(List<GroupEntity> groupList) {
        this.groupList = groupList;
        this.matchGroupList = groupList;
    }

    public void recover(){
        isSearchMode = false;
        matchGroupList = groupList;
        notifyDataSetChanged();
    }

    public void onSearch(String key){
       isSearchMode = true;
       searchKey = key;
        List<GroupEntity> searchList = new ArrayList<GroupEntity>();
       for(GroupEntity group:groupList){
            if(IMUIHelper.handleGroupSearch(searchKey,group)){
                searchList.add(group);
            }
        }
        matchGroupList = searchList;
        notifyDataSetChanged();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        GroupEntity group = (GroupEntity) getItem(position);
        GroupHolder viewHolder = (GroupHolder) view.getTag();

        if (viewHolder == null) {
            return;
        }
        if(currentGroupId!=null){
            holderMap.get(currentGroupId).checkBox.setChecked(false);
        }
        viewHolder.checkBox.toggle();
        boolean checked = viewHolder.checkBox.isChecked();
        if (checked) {
            currentGroupId = group.getId();
        } else {
            currentGroupId = null;
        }

    }

    @Override
    public int getCount() {
       return matchGroupList.size();
    }

    @Override
    public Object getItem(int position) {
        return matchGroupList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        GroupEntity groupEntity = (GroupEntity) getItem(position);
        if(groupEntity == null){
            logger.e("GroupSelectAdapter#getView#groupEntity is null!position:%d",position);
            return null;
        }

        GroupHolder groupHolder = null;
        if (view == null) {
            groupHolder = new GroupHolder();
            view = LayoutInflater.from(ctx).inflate(R.layout.group_select_item, parent,false);
            groupHolder.nameView = (TextView) view.findViewById(R.id.txt_group_name);
            groupHolder.avatar = (IMGroupAvatar)view.findViewById(R.id.iv_head);
            groupHolder.checkBox = (CheckBox) view.findViewById(R.id.checkBox);
            groupHolder.divider = view.findViewById(R.id.group_divider);
            view.setTag(groupHolder);
        } else {
            groupHolder = (GroupHolder) view.getTag();
        }
        holderMap.put(groupEntity.getId(),groupHolder);

        if(groupEntity.getId().equals(currentGroupId)){
            groupHolder.checkBox.setChecked(true);
        }else{
            groupHolder.checkBox.setChecked(false);
        }

        if(isSearchMode){
            // 高亮显示
            IMUIHelper.setTextHilighted(groupHolder.nameView, groupEntity.getMainName(),
                    groupEntity.getSearchElement());
        }else{
            groupHolder.nameView.setText(groupEntity.getMainName());
        }

        Set<String> userNos = groupEntity.getlistGroupMemberIds();
        List<String> avatarUrlList = new ArrayList<>();
        for(String userNo : userNos){
            User entity = IMContactManager.instance().findContact(userNo);
            if(entity != null){
                avatarUrlList.add(entity.getAvatar());

            }
            if(avatarUrlList.size() >= 9){
                break;
            }
        }
        groupHolder.avatar.setAvatarUrls((ArrayList<String>)avatarUrlList);
        groupHolder.divider.setVisibility(View.VISIBLE);

        // checkBox 状态的设定
        return view;
    }

    public String getSeletedGroupId() {
        return currentGroupId;
    }


    public static class GroupHolder {
        View divider;
        TextView nameView;
        IMGroupAvatar avatar;
        CheckBox checkBox;
    }

}
