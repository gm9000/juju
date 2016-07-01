package com.juju.app.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.R;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.SearchElement;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.service.im.IMService;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.pinyin.PinYinUtil;
import com.juju.app.view.groupchat.IMBaseImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目名称：juju
 * 类描述：单选搜索适配器
 * 创建人：gm
 * 日期：2016/6/30 17:09
 * 版本：V1.0.0
 */
public class SingleCheckAdapter extends BaseAdapter implements SectionIndexer,
        AdapterView.OnItemClickListener {

    private Logger logger = Logger.getLogger(GroupSelectAdapter.class);
    private List<ItemBean> allItemList = new ArrayList<>();
    private List<ItemBean> backupList = new ArrayList<>();


    private boolean isSearchMode = false;
    private String searchKey;
    private Context ctx;
//    private IMService imService;

    private ListenerInfo.OnClickListener onClickListener;

    public SingleCheckAdapter(Context context, ListenerInfo.OnClickListener onClickListener) {
        this.ctx = context;
        this.onClickListener = onClickListener;
    }

    public void setAllItemList(List<ItemBean> allItemList) {
        this.allItemList = allItemList;
        this.backupList = allItemList;
    }

    public void recover() {
        isSearchMode = false;
        allItemList = backupList;
        notifyDataSetChanged();
    }

    public void onSearch(String key) {
        isSearchMode = true;
        searchKey = key;
        List<ItemBean> searchList = new ArrayList<>();
        for (ItemBean entity : backupList) {
            if (IMUIHelper.handleItemSearch(searchKey, entity)) {
                searchList.add(entity);
            }
        }
        allItemList = searchList;
        notifyDataSetChanged();
    }

    @Override
    public Object[] getSections() {
        return new Object[0];
    }


    // 在搜索模式下，直接返回
    @Override
    public int getPositionForSection(int sectionIndex) {
        logger.d("pinyin#getPositionForSection secton:%d", sectionIndex);
        int index = 0;
        for (ItemBean bean : allItemList) {
            int firstCharacter = bean.getSectionName().charAt(0);
            if (firstCharacter == sectionIndex) {
                logger.d("pinyin#find sectionName");
                return index;
            }
            index++;
        }
        logger.e("pinyin#can't find such section:%d", sectionIndex);
        return -1;
    }


    @Override
    public int getSectionForPosition(int position) {
        return 0;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ItemBean itemBean = (ItemBean) getItem(position);
        if(onClickListener != null) {
            onClickListener.onItemClick(itemBean);
        }
    }

//    @Override
//    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//        ItemBean itemBean = (ItemBean) getItem(position);
//        return true;
//    }

    @Override
    public int getCount() {
        int size = allItemList == null ? 0 : allItemList.size();
        return size;
    }

    @Override
    public Object getItem(int position) {
        return allItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ItemBean itemBean = (ItemBean) getItem(position);
        if (itemBean == null) {
            logger.e("SingleCheckAdapter#getView#itemBean is null!position:%d", position);
            return null;
        }

        UserHolder userHolder = null;
        if (view == null) {
            userHolder = new UserHolder();
            view = LayoutInflater.from(ctx).inflate(R.layout.adapter_single_check, parent, false);
            userHolder.nameView = (TextView) view.findViewById(R.id.contact_item_title);
            userHolder.realNameView = (TextView) view.findViewById(R.id.contact_realname_title);
            userHolder.sectionView = (TextView) view.findViewById(R.id.contact_category_title);
            userHolder.avatar = (IMBaseImageView) view.findViewById(R.id.contact_portrait);
            userHolder.divider = view.findViewById(R.id.contact_divider);
            view.setTag(userHolder);
        } else {
            userHolder = (UserHolder) view.getTag();
        }

        if (isSearchMode) {
            // 高亮显示
            IMUIHelper.setTextHilighted(userHolder.nameView, itemBean.getMainName(),
                    itemBean.getSearchElement());
        } else {
            userHolder.nameView.setText(itemBean.getMainName());
        }

        userHolder.avatar.setImageResource(R.mipmap.tt_default_user_portrait_corner);
        userHolder.divider.setVisibility(View.VISIBLE);

        // 字母序第一个要展示 ,搜索模式下不展示sectionName
        if (!isSearchMode) {
            String sectionName = itemBean.getSectionName();
            String preSectionName = null;
            if (position > 0) {
                preSectionName = ((ItemBean) getItem(position - 1)).getSectionName();

            }
            if (TextUtils.isEmpty(preSectionName) || !preSectionName.equals(sectionName)) {
                userHolder.sectionView.setVisibility(View.VISIBLE);
                userHolder.sectionView.setText(sectionName);
                userHolder.divider.setVisibility(View.GONE);
            } else {
                userHolder.sectionView.setVisibility(View.GONE);
            }
        } else {
            userHolder.sectionView.setVisibility(View.GONE);
        }

        userHolder.avatar.setDefaultImageRes(R.mipmap.tt_default_user_portrait_corner);
        userHolder.avatar.setCorner(0);
        userHolder.avatar.setImageUrl(itemBean.getAvatar());

        if(StringUtils.isNotBlank(itemBean.getSecName())) {
            userHolder.realNameView.setText(itemBean.getSecName());
            userHolder.realNameView.setVisibility(View.VISIBLE);
        } else {
            userHolder.realNameView.setVisibility(View.GONE);
        }
        return view;
    }


    public static class UserHolder {
        View divider;
        TextView sectionView;
        TextView nameView;
        TextView realNameView;
        IMBaseImageView avatar;
    }


    public static class ListenerInfo {
        public interface OnClickListener {
            void onItemClick(ItemBean itemBean);
        }
    }



    @JsonIgnoreProperties(value = {"pinyinElement", "searchElement", "sectionName"})
    public static class ItemBean {


        private String id;
        private String avatar;
        private String mainName;
        private String secName;
        private PinYinUtil.PinYinElement pinyinElement = new PinYinUtil.PinYinElement();
        private SearchElement searchElement = new SearchElement();
        private String sectionName;


        public static ItemBean build4UserEntity(User user) {
            ItemBean itemBean = new ItemBean();
            itemBean.id = user.getUserNo();
            itemBean.avatar = user.getAvatar();
            itemBean.mainName = user.getNickName();
            return itemBean;
        }


        public String getId() {
            return id;
        }

        public String getAvatar() {
            return avatar;
        }

        public String getMainName() {
            return mainName;
        }

        public String getSecName() {
            return secName;
        }

        public PinYinUtil.PinYinElement getPinyinElement() {
            return pinyinElement;
        }

        public SearchElement getSearchElement() {
            return searchElement;
        }

        public String getSectionName() {
            sectionName = "";
            if (TextUtils.isEmpty(pinyinElement.pinyin)) {
                return sectionName;
            }
            sectionName = pinyinElement.pinyin.substring(0, 1);
            return sectionName;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public void setMainName(String mainName) {
            this.mainName = mainName;
        }

        public void setSecName(String secName) {
            this.secName = secName;
        }

        public void setPinyinElement(PinYinUtil.PinYinElement pinyinElement) {
            this.pinyinElement = pinyinElement;
        }

        public void setSearchElement(SearchElement searchElement) {
            this.searchElement = searchElement;
        }

        public void setSectionName(String sectionName) {
            this.sectionName = sectionName;
        }
    }

}
