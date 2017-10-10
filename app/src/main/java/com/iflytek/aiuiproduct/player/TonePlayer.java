package com.iflytek.aiuiproduct.player;

import java.io.IOException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.aiuiproduct.constant.ProductConstant;

/**
 * 提示音播放器。
 * 
 * @author <a href="http://www.xfyun.cn">讯飞开放平台</a>
 * @date 2016年7月23日 上午9:53:10 
 *
 */
class TonePlayer {
	private static final String TAG = ProductConstant.TAG;
	//是否允许播放，默认为true
	private static boolean sEnabled = true;
	private static MediaPlayer player;
	
	private static SoundPool pool;

	public static void setEnabled(boolean enabled){
		sEnabled = enabled;
	}
	
	/**
	 * 播放音频资源。注：如需要取消播放，直接调用返回的MediaPlayer的release()方法。
	 * 播放完成后执行completionRun中run方法中的任务
	 * @param ctx 上下文
	 * @param resId 资源id
	 * @param completionRun 播放完成后的任务
	 * @return
	 */
	public static MediaPlayer play(Context ctx, int resId, Runnable completionRun) {
		//为给定资源ID创建媒体播放器的方便方法，执行完此句，为播放器加载了音频文件，并执行了prepare()方法，如果播放音乐只需要start()
		MediaPlayer player = MediaPlayer.create(ctx, resId);
		playInternal(player, completionRun);
		return player;
	}
	
	public static MediaPlayer play(Context ctx, int resId) {
		MediaPlayer player = MediaPlayer.create(ctx, resId);
		playInternal(player, null);
		return player;
	}
	/**
	 * 播放音频资源。注：如需要取消播放，直接调用返回的MediaPlayer的release()方法。
	 * 
	 * @param ctx 上下文
	 * @param assetsFileName assets资源文件路径
	 * @param completionRun 播放完成后的任务
	 * @return
	 */
	public static MediaPlayer play(Context ctx, String assetsFileName, Runnable completionRun) {
		MediaPlayer player = createMediaPlayer(ctx, assetsFileName);
		playInternal(player, completionRun);
		return player;
	}

	public static MediaPlayer play(Context ctx, String assetsFileName) {
		MediaPlayer player = createMediaPlayer(ctx, assetsFileName);
		playInternal(player, null);
		return player;
	}

	/**
	 * 播放path路径下的音乐looptimes次
	 * @param context
	 * @param path
	 * @param looptimes
	 */
	public static synchronized void playUseSoundPool(Context context, 
			final String path, final int looptimes) {
		try {
			if (null == context) {
				return;
			}
			
			if (null != pool) {
				pool.release();
			} 
			
			pool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
			//打开指定音乐文件
			final AssetFileDescriptor afd = context.getAssets().openFd(path);
			// 加载完成
			pool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
				
				@Override
				public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
					soundPool.play(sampleId, 1, 1, 0, looptimes, 1);
					
					if (null != afd) {
						try {
							afd.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			});
			
			pool.load(afd, 1);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	/**
	 * 播放音乐，播放完执行completionRun.run();
	 * @param player
	 * @param completionRun
	 */
	private static void playInternal(final MediaPlayer player, final Runnable completionRun) {
		if (player == null) { // 播放错误或失败时继续执行下面的操作，避免卡住
			if (completionRun != null) {
				completionRun.run();
			}
			return;
		}
		
		player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if (completionRun != null) {
					completionRun.run();
				}
				mp.release();
			}
		});
		
		player.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.d(TAG, "Error=" + what + ",extra=" + extra);
				player.start();
				return true;
			}
		});
		//开始播放
		player.start();
	}
	/**
	 * 为给定文件创建媒体播放器的方便方法，加载音乐并准备
	 * @param context
	 * @param assetFileName
	 * @return
	 */
	private static MediaPlayer createMediaPlayer(Context context, String assetFileName) {
		MediaPlayer mp = new MediaPlayer();
		try {
			if (assetFileName.contains("/AIUIProductDemo/")) {
				//加载指定的音乐文件
				mp.setDataSource(assetFileName);
				//准备音乐
				mp.prepare();
				return mp;
			} else if (assetFileName.contains("http")) {
				mp.setDataSource(assetFileName);
				mp.prepare();
				return mp;
			} else {
				//打开指定音乐文件
				AssetFileDescriptor afd = context.getAssets().openFd(assetFileName);
				if (afd == null) {
					return null;
				}
				//加载指定的音乐文件
				mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
				afd.close();
				//准备
				mp.prepare();
				return mp;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			mp.release();
		}
		return null;
	}
	/**
	 * 播放assetFileName中的音乐
	 * @param context
	 * @param assetFileName
	 */
	public static void playTone(Context context, String assetFileName) {
		playTone(context, assetFileName, null);
	}
	/**
	 * 播放指定文件中的音乐，播放完执行finishCb.run()可以参考疯狂Android 494页
	 * @param context
	 * 上下文
	 * @param assetFileName
	 * 文件名
	 * @param finishCb
	 * 播放完成后的任务
	 */
	public static synchronized void playTone(Context context, String assetFileName, final Runnable finishCb) {
		if(!sEnabled) return;
		
		if (null != player) {
			player.release();
		}
		
		player = new MediaPlayer();
		try {
			if (!TextUtils.isEmpty(assetFileName)) {
				//打开指定音乐文件
				AssetFileDescriptor afd = context.getAssets().openFd(assetFileName);
				//使用MediaPlayer加载指定的声音文件
				player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			
				if (null != afd) {
					afd.close();
				}
				//播放完成，触发此回调
				player.setOnCompletionListener(new OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer mp) {
						if (null != finishCb) {
							finishCb.run();
						}
						
						mp.release();
						
						player = null;
						Log.d(TAG, "release");
					}
				});
				//准备声音
				player.prepare();
				//播放
				player.start();
			} else {
				player.release();
				player = null;
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IllegalStateException e2) {
			e2.printStackTrace();
		} catch (IOException e3) {
			e3.printStackTrace();
		}
	}
	/**
	 * 停止播放，并释放资源
	 */
	public static void stopPlay() {
		synchronized (TonePlayer.class) {
			if (null != player) {
				player.stop();
				player.release();
				player = null;
			}
		}
	}
	
}
