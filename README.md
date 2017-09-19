# 图片编辑使用
1. 调用方式如下:

```
 /**
     *
     * @param activity
     * @param localPath     需要编辑界面
     * @param outputPath    输入路径
     * @param isShowBtn     是否显示
     * @param data          源界面传入数据
     * @param requestCode   请求码
     */
    public static void start(Activity activity, String localPath, String outputPath, boolean isShowBtn, String data, int requestCode) {

        // 涂鸦参数
        GraffitiParams params = new GraffitiParams();
        // 图片路径
        params.mImagePath = localPath;
        params.mAmplifierScale = 0f;//不使用放大镜
//        params.mSavePath = outputPath;//图片保存目录

        Intent intent = new Intent(activity, EditImageActivity.class);
        intent.putExtra(EditImageActivity.KEY_PARAMS, params);
        intent.putExtra(EditImageActivity.KEY_PARAMS, params);
        intent.putExtra(AppParmers.EDIT_ISSHOWBTN, isShowBtn);
        intent.putExtra(AppParmers.EDIT_FROM_DATA, data);

        activity.startActivityForResult(intent, requestCode);


    }
```


2. 返回参数
	- GraffitiActivity.RESULT_OK  表示请求成功
	- GraffitiActivity.RESULT_ERROR  表示请求失败
	
	- booble isShowbtn  EDIT_ISSHOWBTN
	- string data 		EDIT_FROM_DATA

	请从返回intent中自行获取
	
	
	