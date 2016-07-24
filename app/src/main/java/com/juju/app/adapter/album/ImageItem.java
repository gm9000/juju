
package com.juju.app.adapter.album;

import java.io.Serializable;

/**
 * 项目名称：juju
 * 类描述：图片对象
 * 创建人：gm
 * 日期：2016/7/21 10:54
 * 版本：V1.0.0
 */
@SuppressWarnings("serial")
public class ImageItem implements Serializable {
    private String imageId;
    private String thumbnailPath;
    private String imagePath;
    private boolean isSelected = false;

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
