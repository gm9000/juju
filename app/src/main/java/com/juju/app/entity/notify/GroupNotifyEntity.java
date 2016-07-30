package com.juju.app.entity.notify;


import com.juju.app.entity.base.BaseEntity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;


@Table(name = "notify", onCreated = "CREATE UNIQUE INDEX index_group_id ON notify(id);")
public class GroupNotifyEntity extends BaseEntity implements java.io.Serializable {

	@Column(name = "time")
	protected long time;

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}
}
