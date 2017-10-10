package com.lamost.aiuiproductdemo;

import com.iflytek.aiui.utils.log.DebugLog;
import com.iflytek.aiuiproduct.constant.ProductConstant;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	private static final String TAG = ProductConstant.TAG;
	/*// AIUI服务控制对象
	private AIUIAgent mAIUIAgent;

	private AIUIProcessor mProcessor;*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DebugLog.LogD(TAG, "onCreate");
        
        Intent mIntent = new Intent(MainActivity.this, AIUIProductService.class);
        startService(mIntent);
        
       /* mProcessor = new AIUIProcessor(this);
		// 创建AIUIAgent对象，绑定到AIUIServcie，绑定成功之后服务即为开启状态
		//创建AIUIAgent时传递的参数AIUIListener是用于接受AIUIService抛出事件的监听器
		if(mAIUIAgent == null){
			mAIUIAgent = AIUIAgent.createAgent(this, mProcessor);
		}
		
		mProcessor.setAgent(mAIUIAgent);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		DebugLog.LogD(TAG, "destroy AIUIAgent");

	}
}
