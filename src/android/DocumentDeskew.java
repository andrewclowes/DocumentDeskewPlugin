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
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class DocumentDeskew extends CordovaPlugin {
	
	private static final String TAG = "DocumentDeskew";
	
	Uri imageUri;
	String imageFileName;
	String imageFilePath;
	final int TAKE_PICTURE = 115;
	
	Button deskewButton;
	Button edgeButton;
	Button restartButton;
	ImageView image;
	
	int deskewFlag;
	
	public native int deskewImage(long inputMat, long squareMat, long deskewMat, float scaleFactor);
	
	static Mat input;
	static Mat squareOutput;
	static Mat deskewOutput;
	
	private Activity mActivity;
	private Context mContext;
	
	private String package_name;
	private Resources resources;
	
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
		
		Log.i(TAG, "Setting up layout");
		initView();
		
	  	Log.i(TAG, "Trying to load OpenCV library");
	    if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, mActivity, mOpenCVCallBack))
	    {
	      Log.e(TAG, "Cannot connect to OpenCV Manager");
	    }
	}
	
	public void initView() {
		Log.i(TAG, "DocumentDeskew initView");
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mActivity.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				mActivity.setContentView(resources.getIdentifier("activity_deskew", "layout", package_name));
				
				image = (ImageView)mActivity.findViewById(resources.getIdentifier("deskewImage", "id", package_name));
			    addListenerOnDeskewButton();
			    addListenerOnRestartButton();
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
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException 
    {	
		try 
		{
		    if (action.equals("open")) 
		    { 
		        //JSONObject arg_object = args.getJSONObject(0);
				Log.i(TAG, "DocumentDeskew open");

			    openCamera();
			    
			    callbackContext.success();
			    return true;
		    }
		    
		    callbackContext.error("Invalid action");
		    return false;
		} 
		catch(Exception e) 
		{
		    System.err.println("Exception: " + e.getMessage());
		    callbackContext.error(e.getMessage());
		    return false;
		} 
	}
	
	static 
	{
		try
		{
			System.loadLibrary("gnustl_shared");
	        //To do - add your static code
			Log.v(TAG, "Native code library gnustl_shared loaded.\n");
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
        String fileName = "DeskewPhoto_" + currentDateandTime + ".jpg";        
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
				
	        	    input = new Mat();
	        		squareOutput = new Mat();
	        		deskewOutput = new Mat();
	        		
	        		deskewFlag = 0;
                	
                    photoToMat(input, imageUri);					
                    deletePhoto(imageFilePath);
    				int i = deskewImage(input.getNativeObjAddr(), squareOutput.getNativeObjAddr(), deskewOutput.getNativeObjAddr(), 0.5f); 
    				
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
		deskewButton = (Button)mActivity.findViewById(resources.getIdentifier("deskewButton", "id", package_name));
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
		restartButton = (Button)mActivity.findViewById(resources.getIdentifier("restartButton", "id", package_name));
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
	
	private Bitmap matToBitmap(Mat imageMat)
	{
		Bitmap resultBitmap = Bitmap.createBitmap(imageMat.cols(),  imageMat.rows(),Bitmap.Config.ARGB_8888);;
		Utils.matToBitmap(imageMat, resultBitmap);
		
		return resultBitmap;
	}
	
	private void saveBitmap(Bitmap bm)
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

		//you can create a new file name "test.jpg" in sdcard folder.
		File f = new File(Environment.getExternalStorageDirectory()
		                        + File.separator + "test.jpg");
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
	}
}
