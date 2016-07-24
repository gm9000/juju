package com.juju.app.fastdfs;

/**
 * 项目名称：juju
 * 类描述：存储文件路径信息
 * 创建人：gm
 * 日期：2016/7/22 09:46
 * 版本：V1.0.0
 */
public class StorePath {

    private String group;
    private String path;
    private String url;

    /**
     *
     */
    public StorePath() {
        super();
    }

    public StorePath(String group, String path, String url) {
        super();
        this.group = group;
        this.path = path;
        this.url = url;
    }

    /**
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @param group
     *            the group to set
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path
     *            the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "StorePath{" +
                "group='" + group + '\'' +
                ", path='" + path + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
