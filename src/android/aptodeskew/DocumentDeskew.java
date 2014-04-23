package uk.co.aptosolutions.aptodeskew;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.polites.android.GestureImageView;

public class DocumentDeskew extends CordovaPlugin {
	
	private static final String TAG = "DocumentDeskew";
	
	Uri imageUri;
	String imageFileName;
	String imageFilePath;
	final int TAKE_PICTURE = 115;
	
	Button deskewButton;
	Button edgeButton;
	Button restartButton;
	Button okButton;
	//ImageView image;
	GestureImageView image;
	
	RelativeLayout pluginLayout;
	
	int deskewFlag;
	
	public native int deskewImage(long inputMat, long squareMat, long deskewMat, float scaleFactor);
	
	static Mat input;
	static Mat squareOutput;
	static Mat deskewOutput;
	
	private Activity mActivity;
	private Context mContext;
	
	private String package_name;
	private Resources resources;
	
	float deskewScale = 0.0f;
	
	CordovaWebView myWebView;
	
	private CallbackContext callbackContext = null;
	
	private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(mContext) 
	{
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	            case LoaderCallbackInterface.SUCCESS:
	            {
	                Log.i(TAG, "OpenCV loaded successfully");
	                
	                System.loadLibrary("DocumentDeskew");
	                
	            } break;
	            default:
	            {
	                super.onManagerConnected(status);
	            } break;
	        }
	    }
	};
	
	@SuppressLint("InlinedApi")
	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		
		Log.i(TAG, "Initializing DocumentDeskew");
		super.initialize(cordova, webView);
	  
	  	mActivity = this.cordova.getActivity();
	  	mContext = mActivity.getApplicationContext();
		package_name = mActivity.getApplication().getPackageName();
	  	resources = mActivity.getApplication().getResources();
		
		myWebView = webView;
		
		Log.i(TAG, "Setting up layout");
		//initView();
		
//	  	Log.i(TAG, "Trying to load OpenCV library");
//	    if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, mActivity, mOpenCVCallBack))
//	    {
//	      Log.e(TAG, "Cannot connect to OpenCV Manager");
//	    }
	}
	
	public void initView() {
		Log.i(TAG, "DocumentDeskew initView");
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
			
				//mActivity.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);				
				//mActivity.setContentView(resources.getIdentifier("activity_deskew", "layout", package_name));
				
				//pluginLayout = (RelativeLayout)mActivity.findViewById(resources.getIdentifier("pluginLayout", "id", package_name));
				
				//LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				//pluginLayout = (RelativeLayout)vi.inflate(resources.getIdentifier("pluginLayout", "id", package_name), null);
				
				//myWebView.addView(pluginLayout, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
				
				//image = (GestureImageView)mActivity.findViewById(resources.getIdentifier("deskewImage", "id", package_name));
				
				pluginLayout = new RelativeLayout(mContext);
				pluginLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT));
				pluginLayout.setBackgroundColor(Color.parseColor("#000000"));
				myWebView.addView(pluginLayout);
				
				image = new GestureImageView(mContext);
				
				RelativeLayout.LayoutParams iParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				iParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
				iParams.addRule(RelativeLayout.CENTER_VERTICAL);
				image.setLayoutParams(iParams);
				
				//myWebView.addView(image);
				pluginLayout.addView(image);
				
				addListenerOnRestartButton();
			    addListenerOnDeskewButton();
				addListenerOnOkButton();
			}
		});
	}
	
	@Override
    public void onDestroy() {
		Log.i(TAG, "DocumentDeskew onDestroy" );

		mActivity = null;
		mContext = null;
		
		input = null;
		squareOutput = null;
		deskewOutput = null;
		
		resources = null;
    }
	
	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackId) throws JSONException 
    {	
		try 
		{
		    if (action.equals("open")) 
		    { 
				Log.i(TAG, "DocumentDeskew open");
				
				this.callbackContext = callbackId;
				PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
				result.setKeepCallback(true);
				this.callbackContext.sendPluginResult(result);
				
				initView();
				
				if(args.length() > 0) 
				{
                    deskewScale = (float)args.getDouble(0);
                    if(deskewScale == 0.0f) 
					{
                        PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT, "No value provided for image processing scale factor.");
                        callbackId.sendPluginResult(r);
                        return true;
                    }
                }
				Log.i(TAG, "Deskew Scale Factor: " + deskewScale);

			    openCamera();
			    
			    //callbackId.success();
			    return true;
		    }
		    
		    callbackId.error("Invalid action");
		    return false;
		} 
		catch(Exception e) 
		{
		    System.err.println("Exception: " + e.getMessage());
		    callbackId.error(e.getMessage());
		    return false;
		} 
	}
	
	static 
	{
		try
		{
			if (!OpenCVLoader.initDebug()) 
			{
				Log.e(TAG, "OpenCV native code libraries failed to load.\n");
			} 
			else 
			{
				System.loadLibrary("gnustl_shared");
				System.loadLibrary("DocumentDeskew");
			}
		
			//System.loadLibrary("gnustl_shared");
			//Log.v(TAG, "Native code library gnustl_shared loaded.\n");
	    }
	    catch(UnsatisfiedLinkError e) 
	    {
	        Log.v(TAG, "Native code library failed to load.\n" + e);
	    }         
	    catch(Exception e) 
	    {
	        Log.v(TAG, "Exception: " + e);
	    }
	}
	
	@Override
	public void onPause(boolean multitasking)
	{
	    super.onPause(multitasking);
	}
	
	@Override
	public void onResume(boolean multitasking)
	{
	    super.onResume(multitasking);
	    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, mActivity, mOpenCVCallBack);
	}
	
    @SuppressLint("SimpleDateFormat") 
    private void openCamera()
    {
    	Log.i(TAG,"Opening camera");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String fileName = "InputPhoto_" + currentDateandTime + ".jpg";
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        File photoFile = new File(Environment.getExternalStorageDirectory(),  fileName);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photoFile));
        imageUri = Uri.fromFile(photoFile);
        imageFileName = fileName;
        imageFilePath = photoFile.getAbsolutePath();
        //mActivity.startActivityForResult(intent, TAKE_PICTURE);
		
		this.cordova.startActivityForResult((CordovaPlugin)this, intent, TAKE_PICTURE);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
		Log.i(TAG,"onActivityResult");
	
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) 
        {
            case TAKE_PICTURE:			
                if (resultCode == Activity.RESULT_OK) 
                {    
					Log.i(TAG, "Photo OK");
					
					input = null;
					squareOutput = null;
					deskewOutput = null;
					
					Drawable drawable = image.getDrawable();
	        		if (drawable instanceof BitmapDrawable) {
	        		    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
	        		    Bitmap bitmap = bitmapDrawable.getBitmap();
	        		    bitmap.recycle();
	        		}
				
	        	    input = new Mat();
	        		squareOutput = new Mat();
	        		deskewOutput = new Mat();
	        		
	        		deskewFlag = 0;
                	
                    photoToMat(input, imageUri);
                    //deletePhoto(imageFilePath);
    				int i = deskewImage(input.getNativeObjAddr(), squareOutput.getNativeObjAddr(), deskewOutput.getNativeObjAddr(), /*0.5f*/0.25f); 
    				
    				if (i == 1)
    				{
    					Bitmap resultBitmap = matToBitmap(squareOutput);
    					image.setImageBitmap(resultBitmap);
    					image.invalidate();
    					
    					deskewButton.setVisibility(View.VISIBLE);
    					deskewButton.setText("Deskew");
    				}
    				else
    				{
    					deskewButton.setVisibility(View.GONE);
                    	Toast.makeText(mActivity, "No documents found in image. Please Restart.", Toast.LENGTH_SHORT).show();
    				}
                }
                else if (resultCode == Activity.RESULT_CANCELED)
                {
					Log.i(TAG, "Photo Canceled");
				
    				deletePhoto(imageFilePath);
    				openCamera();
                }
        }
    }
    
    public void addListenerOnDeskewButton() 
	{		  
		//deskewButton = (Button)mActivity.findViewById(resources.getIdentifier("deskewButton", "id", package_name));
		
		deskewButton = new Button(mContext);
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(120, LayoutParams.WRAP_CONTENT);
		//params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.addRule(RelativeLayout.LEFT_OF, restartButton.getId());
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	
		params.setMargins(0, 50, 0, 0);		
		deskewButton.setLayoutParams(params);
		
		deskewButton.setText("Deskew");	
		deskewButton.setId(1000);
		//myWebView.addView(deskewButton);		
		pluginLayout.addView(deskewButton);
		
		deskewButton.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View arg0) 
			{
				if (deskewFlag == 0)
				{
					Log.i(TAG, "Deskew pressed");
				
					Bitmap resultBitmap = matToBitmap(deskewOutput);
					image.setImageBitmap(resultBitmap);
					image.invalidate();
					
					deskewButton.setText("Edge");
					
					deskewFlag = 1;
				}
				else if (deskewFlag == 1)
				{
					Log.i(TAG, "Edge pressed");
					
					Bitmap resultBitmap = matToBitmap(squareOutput);
					image.setImageBitmap(resultBitmap);
					image.invalidate();
					
					deskewButton.setText("Deskew");
					
					deskewFlag = 0;
				}
			}
		});
	}
    
	public void addListenerOnRestartButton() 
	{		  
		//restartButton = (Button)mActivity.findViewById(resources.getIdentifier("restartButton", "id", package_name));
		
		restartButton = new Button(mContext);
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(120, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	
		restartButton.setLayoutParams(params);
		
		restartButton.setText("Restart");	
		restartButton.setId(2000);
		//myWebView.addView(restartButton);
		pluginLayout.addView(restartButton);
		
		restartButton.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View arg0) 
			{
				Log.i(TAG, "Restart pressed");
				deletePhoto(imageFilePath);
				openCamera();
			}
		});
	}
	
	public void addListenerOnOkButton() 
	{		  
		//okButton = (Button)mActivity.findViewById(resources.getIdentifier("okButton", "id", package_name));
		
		okButton = new Button(mContext);
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(120, LayoutParams.WRAP_CONTENT);
		//params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.addRule(RelativeLayout.RIGHT_OF, restartButton.getId());
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);	
		params.setMargins(0, 50, 0, 0);			
		okButton.setLayoutParams(params);
		
		okButton.setText("Accept");
		okButton.setId(3000);
		//myWebView.addView(okButton);
		pluginLayout.addView(okButton);
		
		okButton.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View arg0) 
			{
				Log.i(TAG, "Accept pressed");
				//hideLayout();
				
				Drawable drawable = image.getDrawable();
	        	if (drawable instanceof BitmapDrawable) 
				{
	        		    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
	        		    Bitmap bitmap = bitmapDrawable.getBitmap();
	        		    bitmap.recycle();
	        	}
				
				deletePhoto(imageFilePath);
				
				Bitmap output = matToBitmap(deskewOutput);
				String outputPath = saveBitmap(output);
				output.recycle();
				
				PluginResult result = new PluginResult(PluginResult.Status.OK, outputPath);

				result.setKeepCallback(false);
				if (callbackContext != null) 
				{
					Log.i(TAG, "Sent result: " + result.getMessage());
					callbackContext.sendPluginResult(result);
					//callbackContext.success(outputPath);
					callbackContext = null;
				}
				
				myWebView.removeView(pluginLayout);
			}
		});
	}
	
    private void deletePhoto(String photoPath)
    {
        File fdelete = new File(photoPath);
        if (fdelete.exists()) 
        {
        	if (fdelete.delete()) 
        	{
        		Log.i(TAG, "File Deleted: " + photoPath);
        	} 
        	else 
        	{
        		Log.i(TAG, "File not Deleted: " + photoPath);
        	}
        }
    }
	
	private void photoToMat(Mat imageMat, Uri uri)
	{
		Bitmap bitmap;
		try 
		{
			bitmap = MediaStore.Images.Media.getBitmap(mActivity.getContentResolver(), uri);
			Utils.bitmapToMat(bitmap, imageMat);
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void photoPathToMat(Mat imageMat, String photoPath)
	{
		Bitmap bitmap;
		try 
		{
			File imgFile = new File(photoPath);
            if(imgFile.exists())
			{
                bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            }
			else
			{
				throw new FileNotFoundException();
			}
			
			Utils.bitmapToMat(bitmap, imageMat);
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private Bitmap matToBitmap(Mat imageMat)
	{
		Bitmap resultBitmap = Bitmap.createBitmap(imageMat.cols(),  imageMat.rows(),Bitmap.Config.ARGB_8888);;
		Utils.matToBitmap(imageMat, resultBitmap);
		
		return resultBitmap;
	}
	
	private String saveBitmap(Bitmap bm)
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

		// Create filename fo deskewed photo
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String fileName = "DeskewPhoto_" + currentDateandTime + ".jpg";
		
		//you can create a new file name "test.jpg" in sdcard folder.
		String savePath = Environment.getExternalStorageDirectory()
		                        + File.separator + fileName;
		File f = new File(savePath);
		
		try 
		{
			f.createNewFile();

			//write the bytes in file
			FileOutputStream fo = new FileOutputStream(f);
			fo.write(bytes.toByteArray());

			// remember close de FileOutput
			fo.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		return savePath;
	}
	
	void hideLayout()
	{
		Log.i(TAG, "Hiding Layout");
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				//image.setVisibility(View.GONE);
				
				//deskewButton.setVisibility(View.GONE);
				//restartButton.setVisibility(View.GONE);
				//okButton.setVisibility(View.GONE);
				
				pluginLayout.setVisibility(View.GONE);
			}
		});
	}
	
	void showLayout()
	{
		Log.i(TAG, "Hiding Layout");
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				//image.setVisibility(View.VISIBLE);
				
				//deskewButton.setVisibility(View.VISIBLE);
				//restartButton.setVisibility(View.VISIBLE);
				//okButton.setVisibility(View.VISIBLE);
				
				pluginLayout.setVisibility(View.VISIBLE);
			}
		});
	}
}
