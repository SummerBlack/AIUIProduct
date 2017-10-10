package com.iflytek.aiuiproduct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.aiui.AIUIErrorCode;
import com.iflytek.aiui.servicekit.AIUIAgent;
import com.iflytek.aiui.servicekit.AIUIConstant;
import com.iflytek.aiui.servicekit.AIUIEvent;
import com.iflytek.aiui.servicekit.AIUIListener;
import com.iflytek.aiui.servicekit.AIUIMessage;
import com.iflytek.aiui.utils.log.DebugLog;
import com.iflytek.aiuiproduct.constant.ProductConstant;
import com.iflytek.aiuiproduct.handler.AsrResultHandler;
import com.iflytek.aiuiproduct.handler.IatResultHandler;
import com.iflytek.aiuiproduct.handler.SemanticResultHandler;
import com.iflytek.aiuiproduct.player.InsType;
import com.iflytek.aiuiproduct.player.PlayController;
import com.iflytek.aiuiproduct.player.PlayController.PalyControllerItem;
import com.iflytek.aiuiproduct.player.PlayControllerListenerAdapter;
import com.iflytek.aiuiproduct.utils.AppTimeLogger;
import com.iflytek.aiuiproduct.utils.AppTimeLogger.TimeLog;
import com.iflytek.aiuiproduct.utils.AppTimeLogger.TimeLogSaveListener;
import com.iflytek.aiuiproduct.utils.ConfigUtil;
import com.iflytek.aiuiproduct.utils.DevBoardControlUtil;
import com.iflytek.aiuiproduct.utils.FileUtil;
import com.iflytek.aiuiproduct.utils.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

/**
 * AIUI处理类
 * @author PR
 *
 */
public class AIUIProcessor extends PlayControllerListenerAdapter implements AIUIListener  {
	private static final String TAG = ProductConstant.TAG;
	private static final String AUDIO_DIR = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/Audio/";
	//private FileUtil.DataFileHelper audioHelper = FileUtil.createFileHelper(AUDIO_DIR);
	private final static String WAV_PATH = "wav/";
	private final static String START_SUCCESS = WAV_PATH + "start_success.mp3";
	private final static String TONE_WRONG_APPID = WAV_PATH + "wrong_appid.mp3";

	private final static String XIAOAI_GREETING_EWZN = WAV_PATH + "xiaoai_greeting_ewzn.mp3";
	private final static String XIAOAI_GREETING_GS = WAV_PATH + "xiaoai_greeting_gs.mp3";
	private final static String XIAOAI_GREETING_SS = WAV_PATH + "xiaoai_greeting_ss.mp3";
	private final static String XIAOAI_GREETING_WTZN = WAV_PATH + "xiaoai_greeting_wtzn.mp3";

	private final static String XIAOAI_GOODBYE_NWZL = WAV_PATH + "xiaoai_goodbye_nwzl.mp3";

	// 唤醒后播放的欢迎音频
	private final static String[] WAKE_UP_TONES = {
		XIAOAI_GREETING_EWZN,		// 嗯，我在呢
		XIAOAI_GREETING_GS,			// 干啥
		XIAOAI_GREETING_SS,			// 啥事
		XIAOAI_GREETING_WTZN 		// 我听着呢
	};

	private final static String GRAMMAR_FILE_PATH = "grammar/grammar.bnf";

	/*AIUIAgent对象，绑定到AIUIServcie，绑定成功之后服务即为开启状态，可以与AIUIServcie通信
	AIUIAgent中sendMessage方法用于向AIUIService发送AIUI消息
	AIUIListener中监听AIUIServcie抛出事件的类型是AIUIEvent*/
	private AIUIAgent mAIUIAgent;

	private Context mContext;
	// 音乐文本播放控制对象
	private PlayController mPlayController;
	// 判断是不是处于唤醒状态
	private boolean mIsWakeUp = false;
	//导致休眠的错误码
	private int mSleepErrorCode = 0;
	//语义结果处理
	private SemanticResultHandler mSemanticHandler;
	//离线命令词处理
	private AsrResultHandler mAsrHandler;
	//听写结果处理
	private IatResultHandler mIatResultHandler;
	// 休眠的广播接收者
	private BroadcastReceiver mSleepReceiver;
	//声音控制广播
	private BroadcastReceiver mVoiceCtrReceiver;
	// 切换情景
	private BroadcastReceiver mSceneReceiver;
	//串口
	//private UARTAgent mUARTAgent;
	//拾音束
	private int beam;
	private long wakeupTime;
	private long bos_time;
	// 保存唤醒词
	private SharedPreferences mWakeupNameSP = null;
	private SharedPreferences.Editor mWakeupNameEditor = null;


	public AIUIProcessor(Context context){
		mContext = context;
		// 关闭唤醒方向指示灯
		DevBoardControlUtil.sleepLight();
		// 设置WIFI指示灯
		DevBoardControlUtil.wifiStateLight(NetworkUtil.isNetworkAvailable(mContext));
		// 设置耗时日志监听器
		AppTimeLogger.setTimeLogSaveListener(mLogSaveListener);
		//播放控制器，所有和播放有关的，都靠他实现
		mPlayController = PlayController.getInstance(mContext);
		mPlayController.setPalyControllerListener(this);
		mPlayController.setMaxVolum();
		//语义处理
		mSemanticHandler = new SemanticResultHandler(mContext);
		//离线识别结果处理
		mAsrHandler = new AsrResultHandler(mContext);
		//听写结果处理
		mIatResultHandler = new IatResultHandler(mContext);
		//注册广播监听器
		registerReceiver();
		//保存/初始化唤醒词
		mWakeupNameSP = mContext.getSharedPreferences("wakeupname", Context.MODE_PRIVATE);
		mWakeupNameEditor = mWakeupNameSP.edit();
		/*String wakeup_name = mWakeupNameSP.getString("name", null);
		if (wakeup_name != null) {
			changeIvw(wakeup_name);
		}*/
	}
	/**
	 *
	 * @param agent
	 */
	public void setAgent(AIUIAgent agent){
		mAIUIAgent = agent;
	}
	/**
	 * 注册广播监听器，包括休眠和设置是否播放合成和音乐
	 */
	private void registerReceiver(){
		mSleepReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				resetWakeup(false);
			}
		};
		mContext.registerReceiver(mSleepReceiver, new IntentFilter(ProductConstant.ACTION_SLEEP));

		mVoiceCtrReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String playMode = intent.getStringExtra("play_mode");
				if(playMode.equals("enable")){
					mPlayController.setPlayVoiceEnable(true, true);
				}else if(playMode.equals("disable")){
					mPlayController.setPlayVoiceEnable(false, true);
				}
			}
		};
		mContext.registerReceiver(mVoiceCtrReceiver, new IntentFilter(ProductConstant.ACTION_VOICE_CTRL));
		// 更换场景或唤醒词
		mSceneReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String scene = intent.getStringExtra("scene");
				String wakeupName = intent.getStringExtra("name");
				if (!TextUtils.isEmpty(scene)) {
					changeScene(scene);
				}
				if (!TextUtils.isEmpty(wakeupName)) {
					changeIvw(wakeupName);
					mWakeupNameEditor.putString("name", wakeupName);
					mWakeupNameEditor.commit();
				}
			}
		};
		mContext.registerReceiver(mSceneReceiver, new IntentFilter(ProductConstant.ACTION_SCENE_Name));
	}

	private TimeLogSaveListener mLogSaveListener = new TimeLogSaveListener() {

		@Override
		public void onSave(TimeLog log) {
		if (null != mAIUIAgent) {
			JSONObject jsonLog = log.toJson();

			AIUIMessage logMsg = new AIUIMessage(AIUIConstant.CMD_SEND_LOG, 0, 0,
					jsonLog.toString(), null);
			mAIUIAgent.sendMessage(logMsg);

			DebugLog.LogD(TAG, "saveTimeLog");
		}
		}
	};

	/**
	 * AIUI事件处理方法
	 * @param event
	 */
	@Override
	public void onEvent(AIUIEvent event) {
		switch (event.eventType) {
		// 绑定事件
		case AIUIConstant.EVENT_BIND_SUCCESS:
		{
			DebugLog.LogD(TAG, "EVENT_BIND_SUCESS");

			mPlayController.playTone("", START_SUCCESS);
			// 构建离线语法
			buildGrammar();
			// 初始化唤醒词
			String wakeup_name = mWakeupNameSP.getString("name", null);
			// System.out.println("---名字---" + wakeup_name);
			if (wakeup_name != null) {
				changeIvw(wakeup_name);
			}
		} break;
		// 唤醒事件
		case AIUIConstant.EVENT_WAKEUP:
		{
			DebugLog.LogD(TAG, "EVENT_WAKEUP");

			processWakeup(event);
		} break;
		// 休眠事件
		case AIUIConstant.EVENT_SLEEP:
		{
			DebugLog.LogD(TAG, "EVENT_SLEEP");

			mIsWakeUp = false;

			if(mSleepErrorCode != 0){
				mPlayController.justTTS("", getErrorTip(mSleepErrorCode), false, new Runnable() {

					@Override
					public void run() {
						DevBoardControlUtil.sleepLight();
					}
				});
				mSleepErrorCode = 0;
			}else{
				// 正在播放音乐且为自动休眠，直接熄灯
				if ((mPlayController.isCurrentPlayMusic() || mPlayController.isCurrentTTS()) && event.arg1 ==0) {
					DevBoardControlUtil.sleepLight();
				} else {
					sayGoodbyeThenSleep("");
				}
			}
			// 停止录音
			//stopThrowAudio();
			//audioHelper.closeWriteFile();
		} break;
		//结果事件
		case AIUIConstant.EVENT_RESULT:
		{
			DebugLog.LogD(TAG, "EVENT_RESULT");
			if (!mIsWakeUp) {
				break;
			}
			processResult(event);
		} break;
		//当检测到输入音频的前端点后，会抛出该事件
		case AIUIConstant.EVENT_VAD:
		{
			/*if (event.arg1 == AIUIConstant.VAD_BOS) {// 前端点
				bos_time = System.currentTimeMillis();
				DebugLog.LogD(TAG, "前端点");
				*//*audioHelper.createPcmFile("");
				startThrowAudio(beam);*//*
			}else if (event.arg1 == AIUIConstant.VAD_EOS){// 后端点
				long eos_time = System.currentTimeMillis();
				readFile(AUDIO_DIR + "all.pcm", bos_time, eos_time, "");
				DebugLog.LogD(TAG, "后端点");
				*//*stopThrowAudio();
				audioHelper.closeWriteFile();*//*
			}*/
			//导致声音忽大忽小，影响演示效果
			/*if(mPlayController.isCurrentPlayMusic() || mPlayController.isCurrentTTS() ){
				processVad(event);
			}*/

		} break;
		//出错事件arg1字段为错误码，info字段为错误描述信息。
		case AIUIConstant.EVENT_ERROR:
		{
			int errorCode = event.arg1;
			processError(errorCode);
		} break;

		case AIUIConstant.EVENT_STATE:
		{
			int serviceState = event.arg1;
			if (AIUIConstant.STATE_IDLE == serviceState) {
				DebugLog.LogD(TAG, "STATE_IDLE");
				DevBoardControlUtil.appidErrorLight(false);
			} else if (AIUIConstant.STATE_READY == serviceState) {
				DebugLog.LogD(TAG, "STATE_READY");
				DevBoardControlUtil.sleepLight();
			} else if (AIUIConstant.STATE_WORKING == serviceState) {
				DebugLog.LogD(TAG, "STATE_WORKING");
			}
		} break;

		case AIUIConstant.EVENT_CMD_RETURN: {
			processCmdReturnEvent(event);
		} break;

		case AIUIConstant.EVENT_START_RECORD:{
			// 开始录制音频
			DebugLog.LogD(TAG, "开始录制音频");
		}break;

		case AIUIConstant.EVENT_AUDIO:{
			//16k音频数据在data中，通过键"audio"获取，实时抛出音频数据
			//byte[] audio = event.data.getByteArray("audio");
			//audioHelper.write(audio, true);
		}break;

		case AIUIConstant.EVENT_STOP_RECORD:{
			// 停止录制音频
			DebugLog.LogD(TAG, "停止录制音频");
		}break;

		default:
			break;
		}
	}

	/**
	 * 从fileName文件中读取数据，保存到saveFileName中
	 * @param fileName
	 * @param startTime 起始时间
	 * @param endTime 结束时间
	 * @param saveFileName 保存文件的名称
	 */
	private void readFile(String fileName, long startTime, long endTime, String saveFileName ){
		FileUtil.DataFileHelper vad_audio = FileUtil.createFileHelper(AUDIO_DIR);
		vad_audio.createPcmFile(saveFileName);
		try {
			RandomAccessFile raf = new RandomAccessFile(fileName, "r");
			long currentPartSize = (endTime - startTime) * 32; // 16K*2字节
			long startPosition = (startTime - wakeupTime - 700) * 32; //认为检测到前端点之前的700ms也为有效数据
			raf.seek(startPosition);
			byte[] buffer = new byte[1024];
			int hasRead = 0;
			int length = 0;
			while (length < currentPartSize
					&& (hasRead = raf.read(buffer)) > 0) {
				vad_audio.write(buffer,true);
				// 累计读取的总大小
				length += hasRead;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		vad_audio.closeWriteFile();
	}
	/**
	 * 前后端点检测
	 * @param event
	 */
	private void processVad(AIUIEvent event){
		switch (event.arg1) {
		case AIUIConstant.VAD_BOS:{
			mPlayController.changeVolToLower();
		}break;

		case AIUIConstant.VAD_EOS:{
			mPlayController.recoverVol();
		}break;

		default:
			break;
		}
	}
	/**
	 * 唤醒事件处理
	 * 停止播放音频和合成，打开角度指示灯，随机播放唤醒提示音
	 * @param event
	 */
	private void processWakeup(AIUIEvent event) {
		//暂停播放音乐
		mPlayController.onMusicCommand("", InsType.PAUSE);
		//停止文本合成
		mPlayController.stopTTS();

		/*if (mIsWakeUp == false) {
			// 唤醒后开始录音,如果之前是唤醒状态就不重复创建
			wakeupTime = System.currentTimeMillis();
			FileUtil.delFile(new File(AUDIO_DIR));
			audioHelper.createPcmFile("all");
			startThrowAudio(beam);
		}*/

		if (!mIsWakeUp && ConfigUtil.isSaveAppTimeLog()) {
			AppTimeLogger.onRealWakeup();
			DebugLog.LogD(TAG, "makeWakeDir");
		}

		mIsWakeUp = true;

		try {
			//获得唤醒角度
			JSONObject wakeInfo = new JSONObject(event.info);
			int wakeAngle = wakeInfo.getInt("angle");
			beam = wakeInfo.getInt("beam");

			DebugLog.LogD(TAG, "wakeAngle=" + wakeAngle);
			// 设置唤醒角度，打开角度指示灯
			DevBoardControlUtil.wakeUpLight(wakeAngle);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		// 随机播放唤醒提示音
		mPlayController.playTone("", WAKE_UP_TONES[new Random().nextInt(WAKE_UP_TONES.length)]);
	}
	/**
	 * 重新启动服务
	 * @param resetAIUI 是否重新启动服务
	 */
	private void resetWakeup(boolean resetAIUI) {
		if (null != mAIUIAgent) {
			if (resetAIUI) {
				//服务会立即停止并重新启动，进入到待唤醒状态。
				AIUIMessage resetMsg = new AIUIMessage(
						AIUIConstant.CMD_RESET, 0, 0, "", null);
				mAIUIAgent.sendMessage(resetMsg);

				DebugLog.LogD(TAG, "reset AIUI");
			} else {
				// AIUI服务重置为待唤醒状态,若当前为唤醒状态，发送该消息重置后会抛出EVENT_SLEEP事件。
				AIUIMessage resetWakeupMsg = new AIUIMessage(
						AIUIConstant.CMD_RESET_WAKEUP, 0, 0, "", null);
				mAIUIAgent.sendMessage(resetWakeupMsg);

				DebugLog.LogD(TAG, "reset Wakeup");
			}
		}
	}
	/**
	 * 某条CMD命令对应的返回事件.
	 * 用arg1标识对应的CMD命令，arg2为返回值，0表示成功,info字段为描述信息。
	 * @param event
	 */
	private void processCmdReturnEvent(AIUIEvent event) {
		switch (event.arg1) {

		case AIUIConstant.CMD_BUILD_GRAMMAR: {
			Log.d(TAG, "构建语法成功");
		} break;

		default:
			break;
		}
	}
	/**
	 * 构建离线语法
	 */
	private void buildGrammar() {
		String grammar = FileUtil.readAssetsFile(mContext, GRAMMAR_FILE_PATH);
		AIUIMessage buildGrammar = new AIUIMessage(AIUIConstant.CMD_BUILD_GRAMMAR,
				0, 0, grammar, null);

		mAIUIAgent.sendMessage(buildGrammar);
		Log.d(TAG,"sendMessage start");
	}

	/**
	 *开始录音
	 * 这个好像是开机就会自动录音
	 */
	private void startRecode(){
		AIUIMessage recodeMsg = new AIUIMessage(AIUIConstant.CMD_START_RECORD, 0, 0, "audio", null);
		mAIUIAgent.sendMessage(recodeMsg);
	}

	/**
	 *停止录音
	 */
	private void stopRecode(){
		AIUIMessage recodeMsg = new AIUIMessage(AIUIConstant.CMD_STOP_RECORD, 0, 0, "audio", null);
		mAIUIAgent.sendMessage(recodeMsg);
	}

	/**
	 * 开始抛出识别音频,调用之后，抛出EVENT_AUDIO事件
	 * @param mic 拾音波束编号设置，若当前未唤醒则会使用该波束拾音，若已经处于唤醒状态则mic不起作用
	 */
	private void startThrowAudio(int mic){
		AIUIMessage audioMsg = new AIUIMessage(AIUIConstant.CMD_START_THROW_AUDIO, mic, 0, "", null);
		mAIUIAgent.sendMessage(audioMsg);
	}

	/**
	 * 停止抛出识别音频,调用之后，停止抛出EVENT_AUDIO事件
	 */
	private void stopThrowAudio(){
		AIUIMessage audioMsg = new AIUIMessage(AIUIConstant.CMD_STOP_THROW_AUDIO, 0, 0, "", null);
		mAIUIAgent.sendMessage(audioMsg);
	}

	/**
	 * 动态切换场景，如从控制模式切换到聊天模式
	 * @param scene 必须是后台配置好的场景
	 */
	private void changeScene(String scene){
		String setParams = "{\"global\":{\"scene\":\""+ scene + "\"}}";
		AIUIMessage setMsg = new AIUIMessage(AIUIConstant.CMD_SET_PARAMS, 0 , 0, setParams, null);
		mAIUIAgent.sendMessage(setMsg);
	}

	/**
	 * 动态更换唤醒词
	 * @param name 唤醒词的名字
	 */
	private void changeIvw(String name){
		String ivwParams = "";
		if ("叮咚叮咚".equals(name)) {
			ivwParams = "{\"ivw\":{\"res_type\":\"assets\",\"res_path\":\"ivw/dingdong.jet\"}}";
		} else if ("灵犀灵犀".equals(name)) {
			ivwParams = "{\"ivw\":{\"res_type\":\"assets\",\"res_path\":\"ivw/lingxilingxi.jet\"}}";
		} else if ("兆峰兆峰".equals(name)) {
			ivwParams = "{\"ivw\":{\"res_type\":\"path\",\"res_path\":\"/sdcard/AIUI/ivw/zhaofeng.jet\"}}";
		} else if ("大白大白".equals(name)) {
			ivwParams = "{\"ivw\":{\"res_type\":\"path\",\"res_path\":\"/sdcard/AIUI/ivw/dabai.jet\"}}";
		}

		AIUIMessage setMsg = new AIUIMessage(AIUIConstant.CMD_SET_PARAMS, 0 , 0, ivwParams, null);
		mAIUIAgent.sendMessage(setMsg);
	}

	/**
	 * 出错处理函数。
	 * 对于有些错误，AIUI服务要重置为待唤醒状态，触发sleep事件进行播报错误码
	 * @param errorCode 错误码
	 */
	private void processError(final int errorCode) {
		DebugLog.LogD(TAG, "AIUI error=" + errorCode);

		// 错误提示
		switch (errorCode) {
			case AIUIErrorCode.MSP_ERROR_TIME_OUT:						// 服务器连接超时
			case AIUIErrorCode.MSP_ERROR_NO_RESPONSE_DATA:  			// 等待结果超时
			case AIUIErrorCode.MSP_ERROR_LMOD_RUNTIME_EXCEPTION:		// 16005，需要重启AIUI会话
			case AIUIErrorCode.MSP_ERROR_NOT_FOUND:						// aiui.cfg中scene参数设置出错
			case AIUIErrorCode.ERROR_SERVICE_BINDER_DIED:				// 与AIUIService的绑定断开
			{
				mSleepErrorCode = errorCode;
				// AIUI服务重置为待唤醒状态
				resetWakeup(false);
			} break;
			// appid校验不通过
			case AIUIErrorCode.MSP_ERROR_DB_INVALID_APPID:
			{
				DevBoardControlUtil.appidErrorLight(true);
				mPlayController.playTone("", TONE_WRONG_APPID);
			} break;

			case AIUIErrorCode.ERROR_NO_NETWORK:
			{
				mPlayController.justTTS("", "网络未连接，请连接网络！", false, new Runnable() {

					@Override
					public void run() {
						DevBoardControlUtil.sleepLight();
					}
				});
			}break;
			/*case AIUIErrorCode.MSP_ERROR_NLP_TIMEOUT:
			{
				mPlayController.playText("", "语义结果超时，请稍等一下！");
			}break;*/
			case 11216:
			{
				mPlayController.justTTS("", "AIUI授权不足，停止服务");
			}break;
			case AIUIErrorCode.MSP_ERROR_FAIL:
			{
				mPlayController.justTTS("", "内容开小差，换点别的吧！");
			} break;
		}
	}

	private String getErrorTip(int errorCode){
		switch (errorCode) {
		case AIUIErrorCode.MSP_ERROR_NOT_FOUND:
			return "场景参数设置出错！";
		case AIUIErrorCode.ERROR_SERVICE_BINDER_DIED:
			return "AIUI服务已断开！";
		default:
			return "网络有点问题我去休息了，请稍后再试！";
		}
	}
	/**
	 * 语音识别结果的处理
	 * @param event
	 */
	private void processResult(AIUIEvent event) {
			long posRsltOnArrival = System.currentTimeMillis();
			// 解析业务结果描述参数，结果格式参见《AIUI集成指南》
			try {
				JSONObject bizParamJson = new JSONObject(event.info);
				JSONObject data = bizParamJson.getJSONArray("data").getJSONObject(0);
				JSONObject params = data.getJSONObject("params");
				JSONObject content = data.getJSONArray("content").getJSONObject(0);

				if (content.has("cnt_id")) {
					String cnt_id = content.getString("cnt_id");
					//从data字段中用cnt_id为key，获取业务结果数据
					JSONObject cntJson = new JSONObject(new String(event.data.getByteArray(cnt_id), "utf-8"));
					String sub = params.optString("sub");

					long posRsltParseFinish = System.currentTimeMillis();

					if ("nlp".equals(sub)) {
						//在线语义结果
						JSONObject result = cntJson.getJSONObject("intent");
						mSemanticHandler.handleResult(result, event.data, params.toString(), posRsltOnArrival, posRsltParseFinish);
					} else if ("asr".equals(sub)) {
						// 处理离线语法结果
						JSONObject result = cntJson.getJSONObject("intent");
						mAsrHandler.handleResult(result);
					}else if("iat".equals(sub)){
						//处理听写结果
						JSONObject resultJson = cntJson.getJSONObject("text");
						mIatResultHandler.handleResult(resultJson);
					}
//						后处理结果
//						else if("tpp".equals(sub)) {
//							//if 后处理结果有效， 发送后处理结果确认消息，以重置语义超时计时，延长交互时间
//							//mAIUIAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_TPP_RESULT_ACK, 0, 0, "", null));
//						}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

	/**
	 * 休眠操作。
	 * 停止所有语音播放，播报"那我走了"，之后关灯
	 * @param uuid
	 */
	private void sayGoodbyeThenSleep(final String uuid) {
		DebugLog.LogD(TAG, "gotoSleep");

		mPlayController.stopPlayControl();

		// 播放休眠提示音
		mPlayController.playTone(uuid, XIAOAI_GOODBYE_NWZL, new Runnable() {

			@Override
			public void run() {
				AppTimeLogger.onSleep(uuid);

				DevBoardControlUtil.sleepLight();
			}
		});
	}

	private void unregisterReceiver(){
		mContext.unregisterReceiver(mSleepReceiver);
		mContext.unregisterReceiver(mVoiceCtrReceiver);
	}

	public void destroy(){
		unregisterReceiver();
		mSemanticHandler.destroy();
		mAsrHandler.destroy();
		mIatResultHandler.destroy();
		mAIUIAgent = null;
	}

	@Override
	public void onError(PalyControllerItem playItem, final int errorCode) {
		DebugLog.LogD(TAG, "TTS Error. ErrorCode=" + errorCode);

		processError(errorCode);
	}
}
