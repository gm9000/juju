package com.juju.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.juju.app.R;
import com.juju.app.activity.chat.GroupManagerActivity;
import com.juju.app.activity.chat.GroupMemberSelectActivity;
import com.juju.app.activity.chat.UserInfoActivity;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.PeerEntity;
import com.juju.app.event.notify.RemoveGroupEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.manager.IMContactManager;
import com.juju.app.service.notify.RemoveGroupNotify;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.view.groupchat.IMBaseImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



public class GroupManagerAdapter extends BaseAdapter {
	private Logger logger = Logger.getLogger(GroupManagerAdapter.class);
	private Context context;

    // 用于控制是否是删除状态，也就是那个减号是否出现
	private boolean removeState = false;
    private boolean showMinusTag = false;
    private boolean showPlusTag = false;

	private List<User> memberList = new ArrayList<>();
    private IMService imService;
    private String groupCreatorId = "";
    private PeerEntity peerEntity;
    private UserInfoBean userInfoBean;

	public GroupManagerAdapter(Context c, IMService imService, PeerEntity peerEntity) {
        memberList.clear();
        this.context = c;
		this.imService = imService;
        this.peerEntity = peerEntity;
        this.userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        setData();
	}

    //todo 在选择添加人页面，currentGroupEntity 的值没有设定
	public void setData() {
        int sessionType = peerEntity.getType();
        switch (sessionType){
            case DBConstant.SESSION_TYPE_GROUP:
                GroupEntity groupEntity =  (GroupEntity)peerEntity;
                setGroupData(groupEntity);
                break;
//            case DBConstant.SESSION_TYPE_SINGLE:{
////                setSingleData((User)peerEntity);
//            }break;
        }
        notifyDataSetChanged();
	}

    private void setGroupData(GroupEntity entity){
        String loginId = userInfoBean.getUserNo();
        String ownerId = entity.getMasterId();
        IMContactManager manager = imService.getContactManager();
        for(String memId:entity.getlistGroupMemberIds()){
           User user =  manager.findContact(memId);
           if(user != null){
               if(ownerId.equals(user.getUserNo())){
                   // 群主放在第一个
                   groupCreatorId = ownerId;
                   memberList.add(0, user);
               }else {
                   memberList.add(user);
               }
           }
        }
        //按钮状态的判断
        switch (entity.getGroupType()){
            case DBConstant.GROUP_TYPE_TEMP:{
                if(loginId.equals(entity.getMasterId())){
                    showMinusTag = true;
                    showPlusTag = true;
                }else{
                    //展示 +
                    showPlusTag = true;
                }
            }
            break;
            case DBConstant.GROUP_TYPE_NORMAL:{
                if(loginId .equals(entity.getMasterId())){
                    // 展示加减
                    showMinusTag = true;
                    showPlusTag = true;
                }else{
                    //展示 +
                    showPlusTag = true;
                }
            }
            break;
        }
    }

//    private void setSingleData(User userEntity){
//        if(userEntity != null){
//            memberList.add(userEntity);
//            showPlusTag = true;
//        }
//    }

	public int getCount() {
		if (null != memberList ) {
			int memberListSize = memberList.size();
            if(showPlusTag){
                memberListSize = memberListSize +1;
            }
            // 现在的情况是有减 一定有加
            if(showMinusTag){
                memberListSize = memberListSize +1;
            }
            return memberListSize;
		}
		return 0;
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return position;
	}


	public void removeById(String contactId) {
        for (User contact : memberList) {
            if (contact.getUserNo().equals(contactId)) {
                memberList.remove(contact);
                break;
            }
        }
        notifyDataSetChanged();
	}

	public void add(User contact) {
		removeState = false;
		memberList.add(contact);
		notifyDataSetChanged();
	}

    public void remove(User contact) {
        removeState = false;
        memberList.remove(contact);
        notifyDataSetChanged();
    }

    public void refreshGroupData(GroupEntity groupData) {
        memberList.clear();
        setGroupData(groupData);
        notifyDataSetChanged();
    }

    public void add(List<User> list){
        removeState = false;
        // 群成员的展示没有去重，在收到IMGroupChangeMemberNotify 可能会造成重复数据
        for(User userEntity:list){
            if(!memberList.contains(userEntity)){
                memberList.add(userEntity);
            }
        }
        notifyDataSetChanged();
    }


	public View getView(int position, View convertView, ViewGroup parent) {
		logger.d("debug#getView position:%d, member size:%d", position, memberList.size());

		GroupHolder holder;
        if(convertView==null)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.adapter_group_manage_grid_item, null);

            holder = new GroupHolder();
            holder.imageView =  (IMBaseImageView) convertView.findViewById(R.id.grid_item_image);
            holder.userTitle = (TextView) convertView.findViewById(R.id.group_manager_user_title);
            holder.role = (ImageView)convertView.findViewById(R.id.grid_item_image_role);
            holder.deleteImg = convertView.findViewById(R.id.deleteLayout);
            holder.imageView.setDefaultImageRes(R.mipmap.tt_default_user_portrait_corner);
            convertView.setTag(holder);
        }
        else
        {
            holder = (GroupHolder)convertView.getTag();
        }

        holder.role.setVisibility(View.GONE);
		if (position >= 0 && memberList.size() > position) {
			logger.d("groupmgr#in mebers area");
			final User userEntity = memberList.get(position);
			setHolder(holder, position, userEntity.getAvatar(), 0, userEntity.getNickName(), userEntity);
			
			if (holder.imageView != null) {
				holder.imageView.setOnClickListener( new OnClickListener() {
					@Override
					public void onClick(View v) {
//						IMUIHelper.openUserProfileActivity(context, userEntity.getPeerId());
                        ActivityUtil.startActivityNew(context,
                                UserInfoActivity.class, Constants.KEY_PEERID, userEntity.getUserNo());
					}
				});
			}
            if(groupCreatorId.equals(userEntity.getUserNo())){
                holder.role.setVisibility(View.VISIBLE);
            }

            if (removeState && !userEntity.getUserNo().equals(groupCreatorId)) {
                holder.deleteImg.setVisibility(View.VISIBLE);
            } else {
                holder.deleteImg.setVisibility(View.INVISIBLE);
            }

		} else if (position == memberList.size() && showPlusTag) {
			logger.d("groupmgr#onAddMsg + button");

			setHolder(holder, position, null, R.drawable.group_manager_add_user, "", null);
            holder.imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                        logger.d("groupmgr#click onAddMsg MemberButton");
                    ActivityUtil.startActivityNew(context, GroupMemberSelectActivity.class,
                            Constants.SESSION_ID_KEY, peerEntity.getSessionKey());
                }
            });
            holder.deleteImg.setVisibility(View.INVISIBLE);

		} else if (position == memberList.size() + 1 && showMinusTag) {
			logger.d("groupmgr#onAddMsg - button");
			setHolder(holder, position, null, R.drawable.tt_group_manager_delete_user, "", null);

            holder.imageView.setOnClickListener(new OnClickListener() {
                 @Override
                 public void onClick(View view) {
                     logger.d("groupmgr#click delete MemberButton");
                     toggleDeleteIcon();
                 }
            });
            holder.deleteImg.setVisibility(View.INVISIBLE);
		}
		return convertView;
	}

	private void setHolder(final GroupHolder holder, int position,
			String avatarUrl, int avatarResourceId, String name,
			User contactEntity) {
		logger.d("debug#setHolder position:%d", position);

		if (null != holder) {
			 holder.imageView.setAdjustViewBounds(false);
			 holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			if (avatarUrl != null) {
                //头像设置
                holder.imageView.setDefaultImageRes(R.mipmap.tt_default_user_portrait_corner);
                holder.imageView.setCorner(8);
                holder.imageView.setImageResource(R.mipmap.tt_default_user_portrait_corner);
                holder.imageView.setImageUrl(avatarUrl);

            } else {
				logger.d("groupmgr#setimageresid %d", avatarResourceId);
                holder.imageView.setImageId(0);
                holder.imageView.setImageId(avatarResourceId);
                holder.imageView.setImageUrl(avatarUrl);
			}


			holder.contactEntity = contactEntity;
			if (contactEntity != null) {
				logger.d("debug#setHolderContact name:%s", contactEntity.getNickName());


				holder.deleteImg.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
                        if(holder.contactEntity == null){return;}
                        String userId = holder.contactEntity.getUserNo();
//                        removeById(userId);
                        //TODO 放在IMOtherManager处理更合理

                        ((GroupManagerActivity)context).showProgressBar();
                        RemoveGroupEvent.RemoveGroupBean removeGroupBean = RemoveGroupEvent
                                .RemoveGroupBean.valueOf(peerEntity.getId(), peerEntity.getMainName(), userId);
                        RemoveGroupNotify.instance().executeCommand4Send(removeGroupBean);

					}
				});
			}
			holder.userTitle.setText(name);
			holder.imageView.setVisibility(View.VISIBLE);
			holder.userTitle.setVisibility(View.VISIBLE);
		}
	}

	
	final class GroupHolder {
		IMBaseImageView imageView;
		TextView userTitle;
		View deleteImg;
		User contactEntity;
        ImageView role;
	}

    public void toggleDeleteIcon(){
        removeState = !removeState;
        notifyDataSetChanged();
    }



//    public interface LoadInfo {
//        void _loading(Object... objs);
//        void _completeLoading(Integer objs);
//    }


}
