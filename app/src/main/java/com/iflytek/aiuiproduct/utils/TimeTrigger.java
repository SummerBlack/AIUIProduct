package com.iflytek.aiuiproduct.utils;

import java.util.Timer;
import java.util.TimerTask;

import com.iflytek.aiui.utils.log.DebugLog;
/*使用时间触发器的一般步骤
 * 1.创建时间触发器对象:new TimeTrigger(time)
 * 2.添加监听器，监听器的onTrigger()方法用于执行触发后的任务
 * 3.开启触发器 start()*/
/**
 * 时间触发器，可在一定延时之后触发
 * 
 * @author <a href="http://www.xfyun.cn">讯飞开放平台</a>
 * @date 2016年7月23日 上午10:30:09
 *
 */

public class TimeTrigger {
	private static final String TAG = "TimeTrigger";
	
	public interface TriggerListener {
		public void onTrigger();
	}
	private long mDelay;

	private Timer mTimer;

	private TimerTask mTimerTask;

	private TriggerListener mListener;
	
	class TriggerTask extends TimerTask {//任务对象
		/**
		 * 延时xx毫秒后会执行Run方法，run方法中执行任务，执行完，会调用监听器的onTrigger()方法
		 * 从run方法中可以看出，其实就是关掉触发器，如果有监听器，执行监听器中的方法，所以真正的任务放在onTrigger()中
		 */
		@Override
		public void run() {
			mTimer.cancel();
			mTimer = null;
			mTimerTask = null;

			if (null != mListener) {
				mListener.onTrigger();//
			}
		}
	}



	/**
	 * 构建函数
	 * 
	 * @param delay 延时，单位：ms。将在delay之后触发
	 */
	public TimeTrigger(long delay) {
		mDelay = delay;
	}
	/**
	 * 添加监听器，当到达指定延时后会执行监听器的onTrigger()方法
	 * @param listener
	 */
	public void setListener(TriggerListener listener) {
		mListener = listener;
	}

	/**
	 * 开启触发器，延时mDelay毫秒后执行mTimerTask中的run()方法
	 */
	public void start() {
		if (null == mTimer) {
			mTimer = new Timer();
			mTimerTask = new TriggerTask();
			mTimer.schedule(mTimerTask, mDelay);
			
			DebugLog.LogD(TAG, "TimeTrigger start");
		}
	}

	/**
	 * 重置触发器到初始状态，重新计时，类似于喂看门狗
	 * 
	 * @return 触发器开启时返回true，关闭状态下返回false
	 */
	public boolean reset() {
		if (null != mTimer) {
			if (null != mTimerTask) {
				mTimerTask.cancel();
				mTimerTask = new TriggerTask();
				mTimer.schedule(mTimerTask, mDelay);
				
				DebugLog.LogD(TAG, "TimeTrigger reset");
				return true;
			}
		}
		return false;
	}

	/**
	 * 取消触发器
	 */
	public void cancel() {
		if (null != mTimer) {
			mTimer.cancel();
			mTimerTask.cancel();

			mTimer = null;
			mTimerTask = null;
			
			DebugLog.LogD(TAG, "TimeTrigger cancel");
		}
	}
	/*使用时间触发器的一般步骤
	 * 1.创建时间触发器对象
	 * 2.添加监听器，监听器的onTrigger()方法用于执行触发后的任务
	 * 3.开启触发器
	 * 该设计思路类似与命令模式：某种方法需要完成某个行为，但该行为必须执行的时候才能确定。
	 * lightTimeTrigger = new TimeTrigger(time);
	   lightTimeTrigger.setListener(new TriggerListener() {
		
		@Override
		public void onTrigger() {
			BoardController.setRGBLight(COLOR_GREEN);
		}
	    });
	    lightTimeTrigger.start();*/
}
