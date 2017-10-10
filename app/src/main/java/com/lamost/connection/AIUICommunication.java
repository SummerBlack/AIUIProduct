package com.lamost.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.lamost.ir.db.ETDB;
import com.lamost.ir.etclass.ETDevice;
import com.lamost.ir.etclass.ETDeviceAIR;
import com.lamost.ir.etclass.ETDeviceTV;
import com.lamost.webservice.ElectricForVoice;
import com.lamost.webservice.SceneDataInfo;
import com.lamost.webservice.WebService;

import et.song.device.DeviceType;

public class AIUICommunication {

	public final static String TAG = AIUICommunication.class.getSimpleName();
	// 将Context移至需要的方法中
	// private static Context mContext = null;
	public final static int NO_ACK = 1;
	public final static int NO_HOST = 2;
	// 使用一个类变量来缓冲曾经创建的实例
	private static AIUICommunication instance = null;
	// 本应用的udp端口号
	private final static int UDP_PORT = 33333;
	// UDP广播搜索主节点IP，广播端口号,主节点收到后会给回复ip/端口号、mac地址等信息
	private final static int UDP_BROADCAST_PORT = 48899;
	// TCP连接主节点，与主节点通信端口号
	private final static int TCP_CONNECT_PORT = 8899;
	// UDP接收主节点IP存储
	private byte[] mReceiveBuffer = new byte[1024];
	// udpSocket主要用于搜索主节点ip
	private DatagramSocket mSearchSocket = null;
	// 主节点的IP
	private InetAddress mInetAddr = null;
	// 主节点编码
	private String mMasterCode = null;
	// 局域网的广播ip
	private final static String netIp = "255.255.255.255";
	// TCP Socket
	private Socket socket = null;
	// 该线程所处理的Socket所对应的输出流
	private OutputStream mOutputStream = null;
	// 该线程所处理的Socket所对应的输入流
	private InputStream mInputStream = null;
	// 标识是否收到主节点正确回复,即是否成功搜索到主节点
	private boolean flag = false;
	// 重新发送次数
	private int try_SendCount = 0;
	// 发送控制指令时最大重发次数
	private static final int MAX_FailedCount = 1;// 之前为3次，现在改为2次，因为每次若没有返回则耗时较长
	// 访问云端获取设备信息
	private WebService mWebService = null;
	// 保存云端获取的设备信息
	private List<ElectricForVoice> electricList = null;
	// 保存情景模式有关信息
	private List<SceneDataInfo> sceneList = null;
	// 保存空调有关信息
	private List<ETDeviceAIR> mDeviceAIRs = new ArrayList<>();
	// 保存电视机有关信息
	private List<ETDeviceTV> mDeviceTVs = new ArrayList<>();
	
	public List<ETDeviceTV> getmDeviceTVs() {
		return mDeviceTVs;
	}

	public List<ETDeviceAIR> getmDeviceAIRs() {
		return mDeviceAIRs;
	}

	public List<SceneDataInfo> getSceneList() {
		return sceneList;
	}

	public void setSceneList(List<SceneDataInfo> sceneList) {
		this.sceneList = sceneList;
	}

	private AIUICommunication() throws SocketException {
		mSearchSocket = new DatagramSocket(UDP_PORT);
		mWebService = new WebService();
	}

	public static synchronized AIUICommunication getInstance()
			throws SocketException {
		if (instance == null) {
			instance = new AIUICommunication();
		}
		return instance;
	}

	/**
	 * 发送字符串"HF-A11ASSISTHREAD"，搜索网关（主节点）
	 * 
	 * @param bytes
	 * @throws IOException
	 */
	private void udpSearchSend() throws IOException {
		// LogMgr.i(TAG, "master udpApiSend :"+mInetAddr +
		// ",port:"+UDP_ROBOT_API_PORT);
		if (mSearchSocket == null || mSearchSocket.isClosed()) {
			mSearchSocket = new DatagramSocket(UDP_PORT);
		}

		byte[] buff = "HF-A11ASSISTHREAD".getBytes();
		DatagramPacket dp = new DatagramPacket(buff, buff.length,
				InetAddress.getByName(netIp), UDP_BROADCAST_PORT);
		mSearchSocket.send(dp);
	}

	/**
	 * 接收网关的回复信息，进而获得网关的ip,同时搜索到主节点后就搜索主节点的编号
	 * 
	 * @param bytes
	 * @throws Exception
	 */
	private Boolean udpServerReceiveSearch() {

		try {
			Arrays.fill(mReceiveBuffer, (byte) 0);
			DatagramPacket inPackage = new DatagramPacket(mReceiveBuffer,
					mReceiveBuffer.length);

			mSearchSocket.setSoTimeout(3000);

			mSearchSocket.receive(inPackage);
			int port = inPackage.getPort();
			if (port == UDP_BROADCAST_PORT) {
				mInetAddr = inPackage.getAddress();
				flag = true;
				// 搜索到主节点ip后就搜索主节点的编号
				tcpSearchMasterCode();
				return true;
			}
		} catch (IOException e) {
			Log.e(TAG, "uudpClientReceiveSearch outtime ");
			return false;
		}
		return false;
	}

	/**
	 * 关闭
	 */
	private void closeUdpSearch() {
		if (mSearchSocket != null && !mSearchSocket.isClosed()) {
			mSearchSocket.close();
		}
	}

	/**
	 * 搜索主控的编号，如果搜索不到则最多搜索MaxSearchCount次。将来判断有没有搜索到主控的唯一方式是主控编号!= null
	 * 
	 * @param MaxSearchCount
	 *            如果搜索不到的最大搜索次数
	 */
	public void searchHost(int MaxSearchCount) {
		if (instance != null) {
			flag = false;
			/*
			 * mMasterCode = null; mInetAddr = null;
			 */
			boolean statue = false;
			int searchCount = 0;
			while (!statue && searchCount < MaxSearchCount) {// 如果搜索不到家庭网关，会每隔1秒钟搜索,每次搜索最多等待5s,连续MaxSearchCount次没有搜索到，任务搜索失败
				try {
					udpSearchSend();
					statue = udpServerReceiveSearch();
					searchCount++;
					Thread.sleep(1000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			closeUdpSearch();
		}
	}

	/**
	 * 获取主节点的编号
	 * 
	 * @return
	 */
	public String tcpSearchMasterCode() {
		if (mInetAddr != null) {
			try {
				socket = new Socket(mInetAddr, TCP_CONNECT_PORT);
				// socket.setSoTimeout(20000);
				socket.setSoTimeout(3000);
				mOutputStream = socket.getOutputStream();
				mInputStream = socket.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(
						mInputStream));
				String buff = "<00000000U************>" + "\r\n";
				mOutputStream.write((buff).getBytes("utf-8"));

				String line = "";
				while (line.equals("") || !line.startsWith("#")) {
					line = br.readLine();
					mMasterCode = line.substring(1, 9);
					return mMasterCode;
				}

			} catch (IOException e) {
				// e.printStackTrace();
				Log.e(TAG, "TCPReceive outtime");

				return "";
			} finally {
				// finally中的代码总会被执行，即使try或者catch中有return语句
				closeTcp();
			}
		}
		return "";
	}

	/**
	 * 更新相应主机下的电器设备和情景模式
	 * 
	 * @return 更新是否成功
	 */
	public boolean updateElectric(Context mContext) {
		// String code = tcpSearchMasterCode();
		if (mMasterCode != null && mContext != null) {

			electricList = mWebService.selectElectricForVoice(mMasterCode,
					mContext);

			sceneList = mWebService.masterReadScene(mMasterCode);

			if (electricList != null && electricList.size() != 0) {
				mDeviceAIRs.clear();// 首先要清空之前保存的空调设备，否则同一个设备每次更新都会加一次
				mDeviceTVs.clear();
				for (ElectricForVoice ele : electricList) {
					// 如果加载的电器中包含空调
					if (ele != null && ele.getElectricCode().startsWith("09") && ele.getElectricType() == 9) {
						int type = DeviceType.DEVICE_REMOTE_AIR;
						ETDeviceAIR mDevice = (ETDeviceAIR) ETDevice
								.Builder(type);
						mDevice.setmMasterCode(mMasterCode);
						mDevice.setmElectricCode(ele.getElectricCode());
						mDevice.setmElectricIndex(ele.getElectricIndex());
						mDevice.SetName(ele.getElectricName());
						mDevice.setmRoomName(ele.getRoomName());
						mDevice.SetType(type);
						mDevice.SetRes(7);
						mDevice.Load(ETDB.getInstance(mContext));
						mDeviceAIRs.add(mDevice);
					}else if (ele != null && ele.getElectricCode().startsWith("09") && ele.getElectricType() == 12) {
						// 如果加载的电器中为电视机
						int type = DeviceType.DEVICE_REMOTE_TV;
						ETDeviceTV mDevice = (ETDeviceTV) ETDevice
								.Builder(type);
						mDevice.setmMasterCode(mMasterCode);
						mDevice.setmElectricCode(ele.getElectricCode());
						mDevice.setmElectricIndex(ele.getElectricIndex());
						mDevice.SetName(ele.getElectricName());
						mDevice.setmRoomName(ele.getRoomName());
						mDevice.SetType(type);
						mDevice.SetRes(0);
						mDevice.Load(ETDB.getInstance(mContext));
						mDeviceTVs.add(mDevice);
					}
				}
				return true;
			}
		}

		return false;
	}

	/**
	 * 关闭TCP socket
	 */
	private void closeTcp() {
		if (socket != null && socket.isConnected()) {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 通过TCP发送控制指令
	 * 
	 * @param str
	 *            发送的控制指令
	 * @return 发送完命令将收到的反馈返回，如果在设置时间内没有收到回复，或出现其他错误返回""
	 * @throws IOException
	 */
	private String sendCommand(String str) throws IOException {
		if (mInetAddr != null) {
			try {
				socket = new Socket(mInetAddr, TCP_CONNECT_PORT);
				// socket.setSoTimeout(20000);时间太长了
				socket.setSoTimeout(3000);
				mOutputStream = socket.getOutputStream();
				mInputStream = socket.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(
						mInputStream));
				// 3s过后自动关掉socket
				TimerTask task = new TimerTask() {
					@Override
					public void run() {
						// task to run goes here
						closeTcp();
					}
				};
				Timer timer = new Timer();
				long delay = 3000;
				// schedules the task to be run in an interval
				timer.schedule(task, delay);

				mOutputStream.write((str + "\r\n").getBytes("utf-8"));
				Log.e(TAG, "已发送指令：" + str);
				String line = null;

				while (true) {
					if (!socket.isClosed()) {
						line = br.readLine();
						// System.out.println(line);
						if (line != null && line.contains(str.substring(4, 8))) {// <0100CD6DXG0100000000FF>如果包含发送控制指令的地址信息，就认为收到回复了
							return line;
						}
					}
				}
			} catch (SocketTimeoutException e) {
				// e.printStackTrace();
				Log.e(TAG, "TCPReceive outtime");

				return "";
			} finally {
				closeTcp();
			}
		}
		return "";
	}

	/**
	 * 向主节点发送指令，如果发送在指定时间内没有收到回复，则重新发送，最多发送2次，如果还是失败了就会重新搜索主节点1次
	 * 如果执行了整个过程，则耗时：执行控制指令的等待时间2*5s+重新搜索主控的时间1*5=15s，如果网络连接出现问题，整个时间还是比较长的
	 * 
	 * @param str
	 *            发送的指令
	 * @param listener
	 *            事件监听器
	 */
	public void sendCommandToHost(String str, ActionListener listener) {
		if (flag == true) {
			String mBack = null;
			try {
				mBack = sendCommand(str);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "发送指令过程中出错");
			}

			if (!TextUtils.isEmpty(mBack)
					&& mBack.contains(str.substring(4, 8))) {// 目前只判断返回是否有地址，将来可以加入更多具体的判断
				try_SendCount = 0;
				listener.onSuccess();
			} else {
				try_SendCount++;
				if (try_SendCount < MAX_FailedCount) {
					sendCommandToHost(str, listener);
				} else {// 说明发送2次都没有回复，需要作出处理，是不是要重新搜索一下网关？
					try_SendCount = 0;
					searchHost(1);
					listener.onFailed(NO_ACK);
				}
			}

		} else {
			listener.onFailed(NO_HOST);
		}
	}

	public boolean isFlag() {
		return flag;
	}

	public String getmMasterCode() {
		return mMasterCode;
	}

	public List<ElectricForVoice> getElectricList() {
		return electricList;
	}

	/**
	 * 销毁，释放资源
	 */
	public void destory() {
		if (mSearchSocket != null && mSearchSocket.isConnected()) {
			mSearchSocket.close();
		}

		if (socket != null && socket.isConnected()) {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		instance = null;
	}
}
