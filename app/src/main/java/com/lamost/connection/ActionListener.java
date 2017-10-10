package com.lamost.connection;
/**
 * 用于监听事件是否成功。
 * 主要应用有：
 * @author User
 *
 */
public interface ActionListener {
	public void onSuccess();
	public void onFailed(int errorCode);
}
