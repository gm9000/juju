
package com.juju.app.adapter.album;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.juju.app.R;
import com.juju.app.utils.Logger;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：相册适配器
 * 创建人：gm
 * 日期：2016/7/21 11:08
 * 版本：V1.0.0
 */
public class ImageBucketAdapter extends BaseAdapter {

    private Activity act;
    private Logger logger = Logger.getLogger(ImageBucketAdapter.class);
    public static int selectedPosition = -1;

    // 图片集列表
    List<ImageBucket> dataList;
    BitmapCache cache;
    BitmapCache.ImageCallback callback = new BitmapCache.ImageCallback() {
        @Override
        public void imageLoad(ImageView imageView, Bitmap bitmap,
                              Object... params) {
            try {
                if (null != imageView && null != bitmap) {
                    String url = (String) params[0];
                    if (null != url && url.equals((String) imageView.getTag())) {
                        ((ImageView) imageView).setImageBitmap(bitmap);
                    } else {
                        logger.e("callback, bmp not match");
                    }
                } else {
                    logger.e("callback, bmp null");
                }
            } catch (Exception e) {
                logger.e(e.getMessage());
            }
        }
    };

    public ImageBucketAdapter(Activity act, List<ImageBucket> list) {
        this.act = act;
        dataList = list;
        cache = BitmapCache.getInstance();
    }

    @Override
    public int getCount() {
        int count = 0;
        if (null != dataList) {
            count = dataList.size();
        }
        return count;
    }

    @Override
    public Object getItem(int arg0) {
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder = null;
        try {
            if (null == convertView) {
                holder = new Holder();
                convertView = View.inflate(act, R.layout.adapter_image_pick, null);
                holder.iv = (ImageView) convertView.findViewById(R.id.image);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.count = (TextView) convertView.findViewById(R.id.count);
                holder.albumArrow = (ImageView) convertView
                        .findViewById(R.id.im_album_arrow);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }
            ImageBucket item = dataList.get(position);
            holder.count.setText("(" + item.count + ")");
            String nameStr = item.bucketName;
            if (nameStr.length() > 14) {
                nameStr = nameStr.substring(0, 14) + "...";
            }
            holder.name.setText(nameStr);
            if (item.imageList != null && item.imageList.size() > 0) {
                String thumbPath = item.imageList.get(0).getThumbnailPath();
                String sourcePath = item.imageList.get(0).getImagePath();
                holder.iv.setTag(sourcePath);
                Bitmap bmp = cache.getCacheBitmap(thumbPath, sourcePath);
                if (bmp != null) {
                    holder.iv.setImageBitmap(bmp);
                } else {
                    cache.displayBmp(holder.iv, thumbPath, sourcePath, callback);
                }
            } else {
                holder.iv.setImageBitmap(null);
                logger.e("no images in bucket " + item.bucketName);
            }
            if (position == selectedPosition) {
                holder.albumArrow.setImageResource(R.mipmap.tt_album_arrow_sel);
                holder.name.setTextColor(Color.WHITE);
                holder.count.setTextColor(Color.WHITE);
            } else {
                holder.albumArrow.setImageResource(R.mipmap.tt_album_arrow);
                holder.name.setTextColor(Color.BLACK);
                holder.count.setTextColor(R.color.album_list_item_count_color);
            }
            return convertView;
        } catch (Exception e) {
            logger.e(e.getMessage());
            return null;
        }
    }

    class Holder {
        private ImageView iv;
        private TextView name;
        private TextView count;
        private ImageView albumArrow;
    }
}
