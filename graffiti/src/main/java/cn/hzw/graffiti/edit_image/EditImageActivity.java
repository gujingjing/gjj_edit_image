package cn.hzw.graffiti.edit_image;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.forward.androids.utils.ImageUtils;
import cn.forward.androids.utils.LogUtil;
import cn.forward.androids.utils.StatusBarUtil;
import cn.forward.androids.utils.ThreadUtil;
import cn.hzw.graffiti.ColorPickerDialog;
import cn.hzw.graffiti.DialogController;
import cn.hzw.graffiti.DrawUtil;
import cn.hzw.graffiti.GraffitiBitmap;
import cn.hzw.graffiti.GraffitiColor;
import cn.hzw.graffiti.GraffitiListener;
import cn.hzw.graffiti.GraffitiParams;
import cn.hzw.graffiti.GraffitiSelectableItem;
import cn.hzw.graffiti.GraffitiText;
import cn.hzw.graffiti.GraffitiView;
import cn.hzw.graffiti.R;
import cn.hzw.graffiti.imagepicker.ImageSelectorView;


/**
 * 涂鸦界面，根据GraffitiView的接口，提供页面交互
 * （这边代码和ui比较粗糙，主要目的是告诉大家GraffitiView的接口具体能实现什么功能，实际需求中的ui和交互需另提别论）
 * Created by huangziwei(154330138@qq.com) on 2016/9/3.
 */
public class EditImageActivity extends Activity implements DialogFragmentDataCallback, CropImageView.OnCropImageCompleteListener {

    public static final String TAG = "Graffiti";

    public static final int RESULT_ERROR = -111; // 出现错误

    /**
     * @param activity
     * @param localPath   需要编辑界面
     * @param outputPath  输入路径
     * @param isShowBtn   是否显示
     * @param data        源界面传入数据
     * @param requestCode 请求码
     */
    public static void start(Activity activity, String localPath, String outputPath, boolean isShowBtn, String data, int requestCode) {

        // 涂鸦参数
        GraffitiParams params = new GraffitiParams();
        // 图片路径
        params.mImagePath = localPath;
        params.mAmplifierScale = 0f;//不使用放大镜
        params.mSavePath = outputPath;//图片保存目录

        Intent intent = new Intent(activity, EditImageActivity.class);
        intent.putExtra(EditImageActivity.KEY_PARAMS, params);
        intent.putExtra(AppParmers.EDIT_ISSHOWBTN, isShowBtn);
        intent.putExtra(AppParmers.EDIT_FROM_DATA, data);

        activity.startActivityForResult(intent, requestCode);


    }

    /**
     * 启动涂鸦界面
     *
     * @param activity
     * @param params      涂鸦参数
     * @param requestCode startActivityForResult的请求码
     * @see GraffitiParams
     */
    public static void startActivityForResult(Activity activity, GraffitiParams params, int requestCode) {
        Intent intent = new Intent(activity, EditImageActivity.class);
        intent.putExtra(EditImageActivity.KEY_PARAMS, params);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 启动涂鸦界面
     *
     * @param activity
     * @param imagePath   　图片路径
     * @param savePath    　保存路径
     * @param isDir       　保存路径是否为目录
     * @param requestCode 　startActivityForResult的请求码
     */
    @Deprecated
    public static void startActivityForResult(Activity activity, String imagePath, String savePath, boolean isDir, int requestCode) {
        GraffitiParams params = new GraffitiParams();
        params.mImagePath = imagePath;
        params.mSavePath = savePath;
        params.mSavePathIsDir = isDir;
        startActivityForResult(activity, params, requestCode);
    }

    /**
     * {@link EditImageActivity#startActivityForResult(Activity, String, String, boolean, int)}
     */
    @Deprecated
    public static void startActivityForResult(Activity activity, String imagePath, int requestCode) {
        GraffitiParams params = new GraffitiParams();
        params.mImagePath = imagePath;
        startActivityForResult(activity, params, requestCode);
    }


    public static final String KEY_PARAMS = "key_graffiti_params";
    public static final String KEY_IMAGE_PATH = "key_image_path";

    private String mImagePath;
    private Bitmap mBitmap;

    private FrameLayout mFrameLayout;
    private GraffitiView mGraffitiView;

    private View.OnClickListener mOnClickListener, mColorOnclickListener, mMenuOnClickListener, mScreenOnclickListener;

    private SeekBar mPaintSizeBar;//调节画笔大小
    private TextView mPaintSizeView;

    private View mBtnColor;
    private Runnable mUpdateScale;

    private int mTouchMode;
    private boolean mIsMovingPic = false;

    // 手势操作相关
    private float mOldScale, mOldDist, mNewDist, mToucheCentreXOnGraffiti,
            mToucheCentreYOnGraffiti, mTouchCentreX, mTouchCentreY;// 双指距离

    private float mTouchLastX, mTouchLastY;

    private boolean mIsScaling = false;
    private float mScale = 1;
    private final float mMaxScale = 3.5f; // 最大缩放倍数
    private final float mMinScale = 0.25f; // 最小缩放倍数
    private final int TIME_SPAN = 40;
    private View mBtnMovePic, mBtnHidePanel, mSettingsPanel;
    private View mShapeModeContainer;
    private View mSelectedTextEditContainer;//文字编辑菜单
    private View mEditContainer;

    private int mTouchSlop;

    private AlphaAnimation mViewShowAnimation, mViewHideAnimation; // view隐藏和显示时用到的渐变动画

    // 当前屏幕中心点对应在GraffitiView中的点的坐标
    float mCenterXOnGraffiti;
    float mCenterYOnGraffiti;

    private GraffitiParams mGraffitiParams;

    // 触摸屏幕超过一定时间才判断为需要隐藏设置面板
    private Runnable mHideDelayRunnable;
    //触摸屏幕超过一定时间才判断为需要隐藏设置面板
    private Runnable mShowDelayRunnable;

//    private float mSelectableItemSize;

    //屏幕宽高
    private int screenWidth;
    private int screenHeight;

    //自定义添加的view
    private IconFontRadioButton rd_write, rd_text, rd_mosaic, rd_screen, rd_back, rd_delete;
    //点击文字，记录
    private GraffitiText currentTextGraffitiText;
    //涂鸦画笔默认大小
    private float drawPaintSize = 13f;
    //文字默认大小
    private int textPaintSize;
    //马赛克大小
    private int mosicaPaintSize;

    //底部菜单  颜色菜单   颜色合集   撤销按钮
    private LinearLayout ll_button_menu, ll_button_color, rg_bottom, ll_back;
    //顶部取消、确认
    private RelativeLayout rl_top,rl_bottom;
    private TextView tv_ensure, tv_cancel, tv_color_line;
    private ImageView red_image, origin_image, green_image, blue_image, white_image, black_image;
    private LinearLayout rg_bottom_group;
    private CropImageView cropImageView;
    private IconFontRadioButton btn_back, btn_cancel, btn_ok;
    private TextView tv_screen_back;
    private RelativeLayout rl_screen;

    //截屏需要的

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_PARAMS, mGraffitiParams);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        mGraffitiParams = savedInstanceState.getParcelable(KEY_PARAMS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setStatusBarTranslucent(this, true, false);
        if (mGraffitiParams == null) {
            mGraffitiParams = getIntent().getExtras().getParcelable(KEY_PARAMS);
        }
        if (mGraffitiParams == null) {
            LogUtil.e("TAG", "mGraffitiParams is null!");
            this.finish();
            return;
        }
        WindowManager wm = (WindowManager) this
                .getSystemService(Context.WINDOW_SERVICE);
        screenWidth = wm.getDefaultDisplay().getWidth();
        screenHeight = wm.getDefaultDisplay().getHeight();

        textPaintSize = DensityUtil.dip2px(this, 30f);
        mosicaPaintSize = DensityUtil.dip2px(this, 60f);

        mImagePath = mGraffitiParams.mImagePath;
        if (mImagePath == null) {
            LogUtil.e("TAG", "mImagePath is null!");
            this.finish();
            return;
        }
        LogUtil.d("TAG", mImagePath);
        if (mGraffitiParams.mIsFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }/*else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }*/
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mBitmap = ImageUtils.createBitmapFromPath(mImagePath, this);
        if (mBitmap == null) {
            LogUtil.e("TAG", "bitmap is null!");
            this.finish();
            return;
        }

        setContentView(R.layout.layout_graffiti_copy);
        mFrameLayout = (FrameLayout) findViewById(R.id.graffiti_container);

        // /storage/emulated/0/DCIM/Graffiti/1479369280029.jpg
        mGraffitiView = new GraffitiView(this, mBitmap, mGraffitiParams.mEraserPath, mGraffitiParams.mEraserImageIsResizeable,
                new GraffitiListener() {
                    @Override
                    public void onSaved(Bitmap bitmap, Bitmap bitmapEraser) { // 保存图片
                        if (bitmapEraser != null) {
                            bitmapEraser.recycle(); // 回收图片，不再涂鸦，避免内存溢出
                        }

                        File graffitiFile = null;
                        File file = null;
                        String savePath = mGraffitiParams.mSavePath;
                        boolean isDir = mGraffitiParams.mSavePathIsDir;
                        if (TextUtils.isEmpty(savePath)) {
                            File dcimFile = new File(Environment.getExternalStorageDirectory(), "DCIM");
                            graffitiFile = new File(dcimFile, "Graffiti");
                            //　保存的路径
                            file = new File(graffitiFile, System.currentTimeMillis() + ".jpg");
                        } else {
                            if (isDir) {
                                graffitiFile = new File(savePath);
                                //　保存的路径
                                file = new File(graffitiFile, System.currentTimeMillis() + ".jpg");
                            } else {
                                file = new File(savePath);
                                graffitiFile = file.getParentFile();
                            }
                        }
                        graffitiFile.mkdirs();

                        FileOutputStream outputStream = null;
                        try {
                            outputStream = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                            ImageUtils.addImage(getContentResolver(), file.getAbsolutePath());
                            Intent intent = getIntent();
                            intent.putExtra(KEY_IMAGE_PATH, file.getAbsolutePath());
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                            onError(GraffitiView.ERROR_SAVE, e.getMessage());
                        } finally {
                            if (outputStream != null) {
                                try {
                                    outputStream.close();
                                } catch (IOException e) {
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(int i, String msg) {
                        setResult(RESULT_ERROR, getIntent());
                        finish();
                    }

                    @Override
                    public void onReady() {
                        mGraffitiView.setPaintSize(mGraffitiParams.mPaintSize > 0 ? mGraffitiParams.mPaintSize
                                : mGraffitiView.getPaintSize());
                        mPaintSizeBar.setProgress((int) (mGraffitiView.getPaintSize() + 0.5f));
                        mPaintSizeBar.setMax((int) (Math.min(mGraffitiView.getBitmapWidthOnView(), mGraffitiView.getBitmapHeightOnView()) / 3 * DrawUtil.GRAFFITI_PIXEL_UNIT));
                        mPaintSizeView.setText("" + mPaintSizeBar.getProgress());

                        //初始化默认值    模拟点击-手绘
//                        rd_write.performClick();
//                        red_image.performClick();

                        mIsMovingPic = true;
                        ll_button_color.setVisibility(View.INVISIBLE);


                    }

                    @Override
                    public void onSelectedItem(GraffitiSelectableItem selectableItem, boolean selected) {
                        if (selected) {

                            //文字显示
                            if (GraffitiView.Pen.TEXT == mGraffitiView.getPen()) {
                                setButtonMenuShow(null, false, true, mGraffitiView.getPen());
                            }
//                            if (mGraffitiView.getSelectedItemColor().getType() == GraffitiColor.Type.BITMAP) {
//                                mBtnColor.setBackgroundDrawable(new BitmapDrawable(mGraffitiView.getSelectedItemColor().getBitmap()));
//                            } else {
//                                mBtnColor.setBackgroundColor(mGraffitiView.getSelectedItemColor().getColor());
//                            }
                            mPaintSizeBar.setProgress((int) (mGraffitiView.getSelectedItemSize() + 0.5f));
                        } else {
                            //隐藏文字
                            if (GraffitiView.Pen.TEXT == mGraffitiView.getPen()) {
                                setButtonMenuShow(null, false, false, mGraffitiView.getPen());
                            }
//                            if (mGraffitiView.getColor().getType() == GraffitiColor.Type.BITMAP) {
//                                mBtnColor.setBackgroundDrawable(new BitmapDrawable(mGraffitiView.getColor().getBitmap()));
//                            } else {
//                                mBtnColor.setBackgroundColor(mGraffitiView.getColor().getColor());
//                            }
//
                            mPaintSizeBar.setProgress((int) (mGraffitiView.getPaintSize() + 0.5f));
                        }
                    }

                    @Override
                    public void onCreateSelectableItem(GraffitiView.Pen pen, float x, float y) {
                        if (pen == GraffitiView.Pen.TEXT) {
                            createGraffitiText(null, x, y);
                        } else if (pen == GraffitiView.Pen.BITMAP) {
                            createGraffitiBitmap(null, x, y);
                        }
                    }
                });
        mGraffitiView.setIsDrawableOutside(mGraffitiParams.mIsDrawableOutside);

        //将图片编辑view加入fragment
        mFrameLayout.addView(mGraffitiView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mOnClickListener = new GraffitiOnClickListener();
        mColorOnclickListener = new ColorOnCLickListener();
        mMenuOnClickListener = new MenuOnClickListener();
        mScreenOnclickListener = new ScreenClickListener();

        mTouchSlop = ViewConfiguration.get(getApplicationContext()).getScaledTouchSlop();
        initView();
    }

    /**
     * 添加文字
     */
    private void createGraffitiText(final GraffitiText graffitiText, final float x, final float y) {

        currentTextGraffitiText = graffitiText;

        Map<String, Object> map = new HashMap<>();
        map.put(AppParmers.X_POSITIONX, x);
        map.put(AppParmers.Y_POSITION, y);
        map.put(AppParmers.TEXT_CONTENT, graffitiText == null ? "" : graffitiText.getText());
        map.put(AppParmers.TEXT_COLOR, mGraffitiView.getGraffitiColor().getColor());

        AddTextDialogFragment commentDialogFragment = AddTextDialogFragment.newInstance(map);
        commentDialogFragment.show(getFragmentManager(), "CommentDialogFragment");

        ll_button_color.setVisibility(View.GONE);

    }

    /**
     * 添加图片
     */
    private void createGraffitiBitmap(final GraffitiBitmap graffitiBitmap, final float x, final float y) {
        Activity activity = this;

        boolean fullScreen = (activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
        Dialog dialog = null;
        if (fullScreen) {
            dialog = new Dialog(activity,
                    android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        } else {
            dialog = new Dialog(activity,
                    android.R.style.Theme_Translucent_NoTitleBar);
        }
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
        ViewGroup container = (ViewGroup) View.inflate(getApplicationContext(), R.layout.graffiti_create_bitmap, null);
        final Dialog finalDialog = dialog;
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalDialog.dismiss();
            }
        });
        dialog.setContentView(container);

        ViewGroup selectorContainer = (ViewGroup) finalDialog.findViewById(R.id.graffiti_image_selector_container);
        ImageSelectorView selectorView = new ImageSelectorView(this, false, 1, null, new ImageSelectorView.ImageSelectorListener() {
            @Override
            public void onCancel() {
                finalDialog.dismiss();
            }

            @Override
            public void onEnter(List<String> pathList) {
                finalDialog.dismiss();
                Bitmap bitmap = ImageUtils.createBitmapFromPath(pathList.get(0), mGraffitiView.getWidth() / 4, mGraffitiView.getHeight() / 4);

                if (graffitiBitmap == null) {
                    mGraffitiView.addSelectableItem(new GraffitiBitmap(mGraffitiView.getPen(), bitmap, mGraffitiView.getPaintSize(), mGraffitiView.getColor().copy(),
                            0, mGraffitiView.getGraffitiRotateDegree(), x, y, mGraffitiView.getOriginalPivotX(), mGraffitiView.getOriginalPivotY()));
                } else {
                    graffitiBitmap.setBitmap(bitmap);
                }
                mGraffitiView.invalidate();
            }
        });
        selectorContainer.addView(selectorView);
    }

    private void initView() {
        //自定义添加的view
        rd_write = (IconFontRadioButton) findViewById(R.id.rd_write);
        rd_text = (IconFontRadioButton) findViewById(R.id.rd_text);
        rd_mosaic = (IconFontRadioButton) findViewById(R.id.rd_mosaic);
        rd_screen = (IconFontRadioButton) findViewById(R.id.rd_screen);
        rd_back = (IconFontRadioButton) findViewById(R.id.rd_back);
        //底部菜单-整体
        ll_button_menu = (LinearLayout) findViewById(R.id.ll_button_menu);
        //颜色菜单
        ll_button_color = (LinearLayout) findViewById(R.id.ll_button_color);
        //撤销按钮
        ll_back = (LinearLayout) findViewById(R.id.ll_back);
        //颜色按钮集合
        rg_bottom = (LinearLayout) findViewById(R.id.rg_bottom);
        //颜色和撤销之间的线
        tv_color_line = (TextView) findViewById(R.id.tv_color_line);
        //删除
        rd_delete = (IconFontRadioButton) findViewById(R.id.rd_delete);
        //rg_bottom_group
        rg_bottom_group = (LinearLayout) findViewById(R.id.rg_bottom_group);


        //顶部取消，确定
        rl_top = (RelativeLayout) findViewById(R.id.rl_top);
        tv_ensure = (TextView) findViewById(R.id.tv_ensure);
        tv_cancel = (TextView) findViewById(R.id.tv_cancel);

        //颜色
        red_image = (ImageView) findViewById(R.id.red_image);
        origin_image = (ImageView) findViewById(R.id.origin_image);
        blue_image = (ImageView) findViewById(R.id.blue_image);
        green_image = (ImageView) findViewById(R.id.green_image);
        white_image = (ImageView) findViewById(R.id.white_image);
        black_image = (ImageView) findViewById(R.id.black_image);

        //
        rl_bottom= (RelativeLayout) findViewById(R.id.rl_bottom);
        //截屏view
        cropImageView = (CropImageView) findViewById(R.id.cropImageView);
//        cropImageView.setScaleType(CropImageView.ScaleType.FIT_CENTER);
        //旋转
        btn_back = (IconFontRadioButton) findViewById(R.id.btn_back);
        //截屏取消
        btn_cancel = (IconFontRadioButton) findViewById(R.id.btn_cancel);
        //截屏完成
        btn_ok = (IconFontRadioButton) findViewById(R.id.btn_ok);
        //截屏撤销
        tv_screen_back = (TextView) findViewById(R.id.tv_screen_back);
        //截屏所有
        rl_screen = (RelativeLayout) findViewById(R.id.rl_screen);

        //截屏事件
        btn_back.setOnClickListener(mScreenOnclickListener);
        btn_cancel.setOnClickListener(mScreenOnclickListener);
        btn_ok.setOnClickListener(mScreenOnclickListener);
        tv_screen_back.setOnClickListener(mScreenOnclickListener);
        rl_bottom.setOnClickListener(mScreenOnclickListener);


        cropImageView.setOnCropImageCompleteListener(this);

        //颜色事件
        red_image.setOnClickListener(mColorOnclickListener);
        origin_image.setOnClickListener(mColorOnclickListener);
        green_image.setOnClickListener(mColorOnclickListener);
        blue_image.setOnClickListener(mColorOnclickListener);
        white_image.setOnClickListener(mColorOnclickListener);
        black_image.setOnClickListener(mColorOnclickListener);

        //顶部取消
        rl_top.setOnClickListener(mMenuOnClickListener);
        //菜单点击事件-防止绘制小点
        ll_button_menu.setOnClickListener(mMenuOnClickListener);
        rg_bottom_group.setOnClickListener(mMenuOnClickListener);
        //删除
        rd_delete.setOnClickListener(mMenuOnClickListener);

        //确定取消事件
        tv_ensure.setOnClickListener(mMenuOnClickListener);
        tv_cancel.setOnClickListener(mMenuOnClickListener);

        rd_text.setOnClickListener(mMenuOnClickListener);
        rd_write.setOnClickListener(mMenuOnClickListener);
        rd_mosaic.setOnClickListener(mMenuOnClickListener);
        rd_screen.setOnClickListener(mMenuOnClickListener);
        rd_back.setOnClickListener(mMenuOnClickListener);


        findViewById(R.id.btn_pen_hand).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_pen_copy).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_pen_eraser).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_pen_text).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_pen_bitmap).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_hand_write).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_arrow).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_line).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_holl_circle).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_fill_circle).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_holl_rect).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_fill_rect).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_clear).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_undo).setOnClickListener(mOnClickListener);
        findViewById(R.id.graffiti_selectable_edit).setOnClickListener(mOnClickListener);
        findViewById(R.id.graffiti_selectable_remove).setOnClickListener(mOnClickListener);
        findViewById(R.id.graffiti_selectable_top).setOnClickListener(mOnClickListener);
        mShapeModeContainer = findViewById(R.id.bar_shape_mode);
        mSelectedTextEditContainer = findViewById(R.id.graffiti_selectable_edit_container);
        mEditContainer = findViewById(R.id.graffiti_edit_container);
        mBtnHidePanel = findViewById(R.id.graffiti_btn_hide_panel);
        mBtnHidePanel.setOnClickListener(mOnClickListener);
        findViewById(R.id.graffiti_btn_finish).setOnClickListener(mOnClickListener);
        findViewById(R.id.graffiti_btn_back).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_centre_pic).setOnClickListener(mOnClickListener);
        mBtnMovePic = findViewById(R.id.btn_move_pic);
        mBtnMovePic.setOnClickListener(mOnClickListener);
        mBtnColor = findViewById(R.id.btn_set_color);
        mBtnColor.setOnClickListener(mOnClickListener);
        mSettingsPanel = findViewById(R.id.graffiti_panel);
        if (mGraffitiView.getGraffitiColor().getType() == GraffitiColor.Type.COLOR) {
            mBtnColor.setBackgroundColor(mGraffitiView.getGraffitiColor().getColor());
        } else if (mGraffitiView.getGraffitiColor().getType() == GraffitiColor.Type.BITMAP) {
            mBtnColor.setBackgroundDrawable(new BitmapDrawable(mGraffitiView.getGraffitiColor().getBitmap()));
        }

        mPaintSizeBar = (SeekBar) findViewById(R.id.paint_size);
        mPaintSizeView = (TextView) findViewById(R.id.paint_size_text);

        mPaintSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    mPaintSizeBar.setProgress(1);
                    return;
                }
                mPaintSizeView.setText("" + progress);
                if (mGraffitiView.isSelectedItem()) {
                    mGraffitiView.setSelectedItemSize(progress);
                } else {
                    mGraffitiView.setPaintSize(progress);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        ScaleOnTouchListener onTouchListener = new ScaleOnTouchListener();
        findViewById(R.id.btn_amplifier).setOnTouchListener(onTouchListener);
        findViewById(R.id.btn_reduce).setOnTouchListener(onTouchListener);

        // 添加涂鸦的触摸监听器，移动图片位置
        mGraffitiView.setOnTouchListener(new View.OnTouchListener() {

            boolean mIsBusy = false; // 避免双指滑动，手指抬起时处理单指事件。

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 隐藏设置面板
                if (!mBtnHidePanel.isSelected()  // 设置面板没有被隐藏
                        && mGraffitiParams.mChangePanelVisibilityDelay > 0) {
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
//                         //触摸屏幕超过一定时间才判断为需要隐藏设置面板
                            ll_button_menu.removeCallbacks(mHideDelayRunnable);
                            ll_button_menu.removeCallbacks(mShowDelayRunnable);
                            ll_button_menu.postDelayed(mHideDelayRunnable, mGraffitiParams.mChangePanelVisibilityDelay);

                            break;
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_UP:
                            //离开屏幕超过一定时间才判断为需要显示设置面板
                            ll_button_menu.removeCallbacks(mHideDelayRunnable);
                            ll_button_menu.removeCallbacks(mShowDelayRunnable);
                            ll_button_menu.postDelayed(mShowDelayRunnable, mGraffitiParams.mChangePanelVisibilityDelay);

                            break;
                    }
                } else if (mBtnHidePanel.isSelected() && mGraffitiView.getAmplifierScale() > 0) {
                    mGraffitiView.setAmplifierScale(-1);
                }

                if (!mIsMovingPic) {
                    return false;  // 交给下一层的涂鸦处理
                }
                mScale = mGraffitiView.getScale();
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchMode = 1;
                        mTouchLastX = event.getX();
                        mTouchLastY = event.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mTouchMode = 0;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (mTouchMode < 2) { // 单点滑动
                            if (mIsBusy) { // 从多点触摸变为单点触摸，忽略该次事件，避免从双指缩放变为单指移动时图片瞬间移动
                                mIsBusy = false;
                                mTouchLastX = event.getX();
                                mTouchLastY = event.getY();
                                return true;
                            }
                            float tranX = event.getX() - mTouchLastX;
                            float tranY = event.getY() - mTouchLastY;
                            mGraffitiView.setTrans(mGraffitiView.getTransX() + tranX, mGraffitiView.getTransY() + tranY);
                            mTouchLastX = event.getX();
                            mTouchLastY = event.getY();
                        } else { // 多点
                            mNewDist = spacing(event);// 两点滑动时的距离
                            if (Math.abs(mNewDist - mOldDist) >= mTouchSlop) {
                                float scale = mNewDist / mOldDist;
                                mScale = mOldScale * scale;

                                if (mScale > mMaxScale) {
                                    mScale = mMaxScale;
                                }
                                if (mScale < mMinScale) { // 最小倍数
                                    mScale = mMinScale;
                                }
                                // 围绕坐标(0,0)缩放图片
                                mGraffitiView.setScale(mScale);
                                // 缩放后，偏移图片，以产生围绕某个点缩放的效果
                                float transX = mGraffitiView.toTransX(mTouchCentreX, mToucheCentreXOnGraffiti);
                                float transY = mGraffitiView.toTransY(mTouchCentreY, mToucheCentreYOnGraffiti);
                                mGraffitiView.setTrans(transX, transY);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_POINTER_UP:
                        mTouchMode -= 1;
                        return true;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mTouchMode += 1;
                        mOldScale = mGraffitiView.getScale();
                        mOldDist = spacing(event);// 两点按下时的距离
                        mTouchCentreX = (event.getX(0) + event.getX(1)) / 2;// 不用减trans
                        mTouchCentreY = (event.getY(0) + event.getY(1)) / 2;
                        mToucheCentreXOnGraffiti = mGraffitiView.toX(mTouchCentreX);
                        mToucheCentreYOnGraffiti = mGraffitiView.toY(mTouchCentreY);
                        mIsBusy = true; // 标志位多点触摸
                        return true;
                }
                return true;
            }
        });

        findViewById(R.id.graffiti_txt_title).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) { // 长按标题栏显示原图
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        mGraffitiView.setJustDrawOriginal(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mGraffitiView.setJustDrawOriginal(false);
                        break;
                }
                return true;
            }
        });

        mViewShowAnimation = new AlphaAnimation(0, 1);
        mViewShowAnimation.setDuration(200);
        mViewHideAnimation = new AlphaAnimation(1, 0);
        mViewHideAnimation.setDuration(200);
        mHideDelayRunnable = new Runnable() {
            public void run() {
//                hideView(mSettingsPanel);
                hideView(ll_button_menu);
                hideView(rl_top);
            }

        };
        mShowDelayRunnable = new Runnable() {
            public void run() {
//                showView(mSettingsPanel);
                showView(ll_button_menu);
                showView(rl_top);
            }
        };

        findViewById(R.id.graffiti_btn_rotate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGraffitiView.rotate(mGraffitiView.getGraffitiRotateDegree() + 90);
            }
        });
    }

    /**
     * 计算两指间的距离
     *
     * @param event
     * @return
     */

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 文字添加回调
     */
    @Override
    public void setCommentText(Map<String, Object> map) {

//        float x = (float) map.get(AppParmers.X_POSITIONX);
//        float y = (float) map.get(AppParmers.Y_POSITION);


        float x = (float) map.get(AppParmers.X_POSITIONX);
        float y = (float) map.get(AppParmers.Y_POSITION);



        int color = (int) map.get(AppParmers.TEXT_COLOR);
        String content = (String) map.get(AppParmers.TEXT_CONTENT);

        if (TextUtils.isEmpty(content)) {
            return;
        }
        int oldColor = mGraffitiView.getColor().getColor();
        setGraffitiViewColor(color);


        if (currentTextGraffitiText == null) {
//            mGraffitiView.addSelectableItem(new GraffitiText(mGraffitiView.getPen(), content, mGraffitiView.getPaintSize(), mGraffitiView.getColor().copy(),
//                    0, mGraffitiView.getGraffitiRotateDegree(), x, y, mGraffitiView.getOriginalPivotX()/2, mGraffitiView.getOriginalPivotY()));

            mGraffitiView.addSelectableItem(new GraffitiText(mGraffitiView.getPen(), content, mGraffitiView.getPaintSize(), mGraffitiView.getColor().copy(),
                    0, mGraffitiView.getGraffitiRotateDegree(), x, y, mGraffitiView.getOriginalPivotX(), mGraffitiView.getOriginalPivotY()));

        } else {
            currentTextGraffitiText.setText(content);
        }
        mGraffitiView.invalidate();

        //设置默认颜色
        mGraffitiView.setColor(oldColor);
    }

    public void setGraffitiViewColor(int color) {
        mGraffitiView.setColor(this.getResources().getColor(color));
    }

    @Override
    public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {

        if (result.getError() == null) {
            Bitmap bitmap = result.getBitmap();
            mGraffitiView.resetBitmap(bitmap);

            rl_screen.setVisibility(View.GONE);
            //设置为滑动模式
            mIsMovingPic = true;

        } else {
            Toast.makeText(EditImageActivity.this, "图片截取失败: " + result.getError().getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    private class ColorOnCLickListener implements View.OnClickListener {
        private View mLastColoriew;

        @Override
        public void onClick(View v) {

            if (v.getId() == R.id.red_image) {//颜色
                setGraffitiViewColor(R.color.red);

            } else if (v.getId() == R.id.origin_image) {
                setGraffitiViewColor(R.color.orange);

            } else if (v.getId() == R.id.green_image) {
                setGraffitiViewColor(R.color.green);
            } else if (v.getId() == R.id.blue_image) {
                setGraffitiViewColor(R.color.blue);
            } else if (v.getId() == R.id.white_image) {
                setGraffitiViewColor(R.color.white);
            } else if (v.getId() == R.id.black_image) {
                setGraffitiViewColor(R.color.black);
            }
            if (mLastColoriew != null) {
                mLastColoriew.setSelected(false);
            }
            v.setSelected(true);
            mLastColoriew = v;
        }
    }

    /**
     * 截屏事件
     */
    private class ScreenClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            if (v.getId() == R.id.btn_back) {//旋转
                cropImageView.rotateImage(90);
            } else if (v.getId() == R.id.btn_cancel) {//取消
                rl_screen.setVisibility(View.GONE);
                //取消刚刚绘制的
                mGraffitiView.undoText();

            } else if (v.getId() == R.id.btn_ok) {//确定
//                cropImageView.getCroppedImageAsync();
                //设置为滑动模式
                LogUtil.e("big_line-btn_ok===");
                mIsMovingPic = true;

                Bitmap cropped = cropImageView.getCroppedImage();

                if (cropped!= null) {
                    mGraffitiView.resetBitmap(cropped);

                    rl_screen.setVisibility(View.GONE);

                } else {
                    Toast.makeText(EditImageActivity.this, "图片截取失败", Toast.LENGTH_LONG).show();
                }

            } else if (v.getId() == R.id.tv_screen_back) {//还原

                cropImageView.setImageBitmap(mGraffitiView.getCurrentScreenBitmap());
            }else if(v.getId()==R.id.rl_bottom){
                LogUtil.e("big_line-rl_bottom===");
            }

        }
    }

    private class MenuOnClickListener implements View.OnClickListener {

        private View lastMenuView;
        private boolean notNeedChangeState;

        @Override
        public void onClick(View v) {

            boolean isselfe = lastMenuView != null && lastMenuView.getId() == v.getId();

            //自定义添加的
            if (v.getId() == R.id.rd_write) {//手绘
                //设置画笔大小
                mGraffitiView.setPaintSize(drawPaintSize);
                //可以设置颜色
                mPaintSizeBar.setProgress((int) (mGraffitiView.getPaintSize()));
                mShapeModeContainer.setVisibility(View.VISIBLE);
                mGraffitiView.setPen(GraffitiView.Pen.HAND);

                mGraffitiView.setShape(GraffitiView.Shape.HAND_WRITE);

                setButtonMenuShow(v, isselfe, true, mGraffitiView.getPen());

            } else if (v.getId() == R.id.rd_text) {//文字

                //                mPaintSizeBar.setProgress(textPaintSize);

                if (mGraffitiView.isSelectedItem()) {
                    mGraffitiView.setSelectedItemSize(textPaintSize);
                } else {
                    mGraffitiView.setPaintSize(textPaintSize);
                }
                mShapeModeContainer.setVisibility(View.GONE);
                mGraffitiView.setPen(GraffitiView.Pen.TEXT);


                //默认在开头中间
//                createGraffitiText(null, screenWidth / 2, screenHeight / 2);
                createGraffitiText(null, -1, -1);

                setButtonMenuShow(v, isselfe, true, mGraffitiView.getPen());


            } else if (v.getId() == R.id.rd_mosaic) {//马赛克
                mGraffitiView.setPen(GraffitiView.Pen.MOSAIC);
                mGraffitiView.setPaintSize(mosicaPaintSize);
                setButtonMenuShow(v, isselfe, true, mGraffitiView.getPen());
//                mGraffitiView.setMosaicResource(mBitmap);


            } else if (v.getId() == R.id.rd_screen) {//截屏
                mGraffitiView.setPen(GraffitiView.Pen.SCREEN);
                setButtonMenuShow(v, isselfe, true, mGraffitiView.getPen());

                rl_screen.setVisibility(View.VISIBLE);
                //设置截屏图片

                mGraffitiView.save(new GraffitiListener() {
                    @Override
                    public void onSaved(Bitmap bitmap, Bitmap bitmapEraser) {
                        cropImageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(int i, String msg) {

                    }

                    @Override
                    public void onReady() {

                    }

                    @Override
                    public void onSelectedItem(GraffitiSelectableItem selectableItem, boolean selected) {

                    }

                    @Override
                    public void onCreateSelectableItem(GraffitiView.Pen pen, float x, float y) {

                    }
                });

                //设置为滑动模式
                mIsMovingPic = true;

            } else if (v.getId() == R.id.rd_back) {//撤销
                mGraffitiView.undo();
            } else if (v.getId() == R.id.tv_cancel) {//取消
                if (!mGraffitiView.isModified()) {
                    finish();
                    return;
                }
                if (!(GraffitiParams.getDialogInterceptor() != null
                        && GraffitiParams.getDialogInterceptor().onShow(EditImageActivity.this, mGraffitiView, GraffitiParams.DialogType.SAVE))) {
                    DialogController.showEnterCancelDialog(EditImageActivity.this, getString(R.string.graffiti_saving_picture), null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mGraffitiView.save();
                        }
                    }, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
                }

            } else if (v.getId() == R.id.tv_ensure) {//确定
                mGraffitiView.save();
            } else if (v.getId() == R.id.rd_delete) {//删除
                mGraffitiView.removeSelectedItem();
                //还是设置文字
                //                mPaintSizeBar.setProgress(textPaintSize);

                if (mGraffitiView.isSelectedItem()) {
                    mGraffitiView.setSelectedItemSize(textPaintSize);
                } else {
                    mGraffitiView.setPaintSize(textPaintSize);
                }
                mShapeModeContainer.setVisibility(View.GONE);
                mGraffitiView.setPen(GraffitiView.Pen.TEXT);

                setButtonMenuShow(v, isselfe, true, mGraffitiView.getPen());

            } else if(v.getId()==R.id.rl_top){//取消，一栏
                //不做处理
                notNeedChangeState = true;
            }else if (v.getId() == R.id.ll_button_menu) {//菜单栏点击事件
                LogUtil.e("big_line-ll_button_menu===");
                //不做处理
                notNeedChangeState = true;
            } else if (v.getId() == R.id.rg_bottom_group) {//radiogroup
                //不做处理
                notNeedChangeState = true;
            }

            //防止外部view北选中状态,影响内部view
            if (!notNeedChangeState) {

                if (lastMenuView != null) {
                    lastMenuView.setSelected(false);
                }
//                v.setSelected(true);
                lastMenuView = v;


            }
            notNeedChangeState = false;
        }
    }

    private class GraffitiOnClickListener implements View.OnClickListener {

        private View mLastPenView, mLastShapeView;
        private boolean mDone = false;

        @Override
        public void onClick(View v) {
            mDone = false;

            if (v.getId() == R.id.btn_pen_hand) {
                mPaintSizeBar.setProgress((int) (mGraffitiView.getPaintSize() + 0.5f));
                mShapeModeContainer.setVisibility(View.VISIBLE);
                mGraffitiView.setPen(GraffitiView.Pen.HAND);
                mDone = true;
            } else if (v.getId() == R.id.btn_pen_copy) {
                mPaintSizeBar.setProgress((int) (mGraffitiView.getPaintSize() + 0.5f));
                mShapeModeContainer.setVisibility(View.VISIBLE);
                mGraffitiView.setPen(GraffitiView.Pen.COPY);
                mDone = true;
            } else if (v.getId() == R.id.btn_pen_eraser) {
                mPaintSizeBar.setProgress((int) (mGraffitiView.getPaintSize() + 0.5f));
                mShapeModeContainer.setVisibility(View.VISIBLE);
                mGraffitiView.setPen(GraffitiView.Pen.ERASER);
                mDone = true;
            } else if (v.getId() == R.id.btn_pen_text) {
                mShapeModeContainer.setVisibility(View.GONE);
                mGraffitiView.setPen(GraffitiView.Pen.TEXT);
                mDone = true;
            } else if (v.getId() == R.id.btn_pen_bitmap) {
                mShapeModeContainer.setVisibility(View.GONE);
                mGraffitiView.setPen(GraffitiView.Pen.BITMAP);
                mDone = true;
            }
            //如果done结束处理
            if (mDone) {
                if (mLastPenView != null) {
                    mLastPenView.setSelected(false);

                }

                v.setSelected(true);
                mLastPenView = v;
                return;
            } else if (v.getId() == R.id.btn_clear) {
                if (!(GraffitiParams.getDialogInterceptor() != null
                        && GraffitiParams.getDialogInterceptor().onShow(EditImageActivity.this, mGraffitiView, GraffitiParams.DialogType.CLEAR_ALL))) {
                    DialogController.showEnterCancelDialog(EditImageActivity.this,
                            getString(R.string.graffiti_clear_screen), getString(R.string.graffiti_cant_undo_after_clearing),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mGraffitiView.clear();
                                }
                            }, null
                    );
                }
                mDone = true;
            } else if (v.getId() == R.id.btn_undo) {
                mGraffitiView.undo();
                mDone = true;
            } else if (v.getId() == R.id.btn_set_color) {
                if (!(GraffitiParams.getDialogInterceptor() != null
                        && GraffitiParams.getDialogInterceptor().onShow(EditImageActivity.this, mGraffitiView, GraffitiParams.DialogType.COLOR_PICKER))) {
                    new ColorPickerDialog(EditImageActivity.this, mGraffitiView.getGraffitiColor().getColor(), "画笔颜色",
                            new ColorPickerDialog.OnColorChangedListener() {
                                public void colorChanged(int color) {
                                    mBtnColor.setBackgroundColor(color);
                                    if (mGraffitiView.isSelectedItem()) {
                                        mGraffitiView.setSelectedItemColor(color);
                                    } else {
                                        mGraffitiView.setColor(color);
                                    }
                                }

                                @Override
                                public void colorChanged(Drawable color) {
                                    mBtnColor.setBackgroundDrawable(color);
                                    if (mGraffitiView.isSelectedItem()) {
                                        mGraffitiView.setSelectedItemColor(ImageUtils.getBitmapFromDrawable(color));
                                    } else {
                                        mGraffitiView.setColor(ImageUtils.getBitmapFromDrawable(color));
                                    }
                                }
                            }).show();
                }
                mDone = true;
            }
            if (mDone) {
                return;
            }

            if (v.getId() == R.id.graffiti_btn_hide_panel) {
                mSettingsPanel.removeCallbacks(mHideDelayRunnable);
                mSettingsPanel.removeCallbacks(mShowDelayRunnable);
                v.setSelected(!v.isSelected());
                if (!mBtnHidePanel.isSelected()) {
                    showView(mSettingsPanel);
                } else {
                    hideView(mSettingsPanel);
                }
                mDone = true;
            } else if (v.getId() == R.id.graffiti_btn_finish) {
                mGraffitiView.save();
                mDone = true;
            } else if (v.getId() == R.id.graffiti_btn_back) {
                if (!mGraffitiView.isModified()) {
                    finish();
                    return;
                }
                if (!(GraffitiParams.getDialogInterceptor() != null
                        && GraffitiParams.getDialogInterceptor().onShow(EditImageActivity.this, mGraffitiView, GraffitiParams.DialogType.SAVE))) {
                    DialogController.showEnterCancelDialog(EditImageActivity.this, getString(R.string.graffiti_saving_picture), null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mGraffitiView.save();
                        }
                    }, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
                }
                mDone = true;
            } else if (v.getId() == R.id.btn_centre_pic) {
                mGraffitiView.centrePic();
                mDone = true;
            } else if (v.getId() == R.id.btn_move_pic) {
                v.setSelected(!v.isSelected());
                mIsMovingPic = v.isSelected();
                if (mIsMovingPic) {
                    Toast.makeText(getApplicationContext(), R.string.graffiti_moving_pic, Toast.LENGTH_SHORT).show();
                }
                mDone = true;
            }
            if (mDone) {
                return;
            }


            if (v.getId() == R.id.graffiti_selectable_edit) {
                if (mGraffitiView.getSelectedItem() instanceof GraffitiText) {
                    createGraffitiText((GraffitiText) mGraffitiView.getSelectedItem(), -1, -1);
                } else if (mGraffitiView.getSelectedItem() instanceof GraffitiBitmap) {
                    createGraffitiBitmap((GraffitiBitmap) mGraffitiView.getSelectedItem(), -1, -1);
                }
                mDone = true;
            } else if (v.getId() == R.id.graffiti_selectable_remove) {
                mGraffitiView.removeSelectedItem();
                mDone = true;
            } else if (v.getId() == R.id.graffiti_selectable_top) {
                mGraffitiView.topSelectedItem();
                mDone = true;
            }
            if (mDone) {
                return;
            }

            if (v.getId() == R.id.btn_hand_write) {
                mGraffitiView.setShape(GraffitiView.Shape.HAND_WRITE);
            } else if (v.getId() == R.id.btn_arrow) {
                mGraffitiView.setShape(GraffitiView.Shape.ARROW);
            } else if (v.getId() == R.id.btn_line) {
                mGraffitiView.setShape(GraffitiView.Shape.LINE);
            } else if (v.getId() == R.id.btn_holl_circle) {
                mGraffitiView.setShape(GraffitiView.Shape.HOLLOW_CIRCLE);
            } else if (v.getId() == R.id.btn_fill_circle) {
                mGraffitiView.setShape(GraffitiView.Shape.FILL_CIRCLE);
            } else if (v.getId() == R.id.btn_holl_rect) {
                mGraffitiView.setShape(GraffitiView.Shape.HOLLOW_RECT);
            } else if (v.getId() == R.id.btn_fill_rect) {
                mGraffitiView.setShape(GraffitiView.Shape.FILL_RECT);
            }

            if (mLastShapeView != null) {
                mLastShapeView.setSelected(false);
            }

            v.setSelected(true);
            mLastShapeView = v;

        }
    }

    @Override
    public void onBackPressed() {

        if (mBtnMovePic.isSelected()) {
            mBtnMovePic.performClick();
            return;
        } else {
            findViewById(R.id.graffiti_btn_back).performClick();
        }

    }

    /**
     * 放大缩小
     */
    private class ScaleOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    scalePic(v);
                    v.setSelected(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mIsScaling = false;
                    v.setSelected(false);
                    break;
            }
            return true;
        }
    }

    /**
     * 缩放
     *
     * @param v
     */
    public void scalePic(View v) {
        if (mIsScaling)
            return;
        mIsScaling = true;
        mScale = mGraffitiView.getScale();

        // 确定当前屏幕中心点对应在GraffitiView中的点的坐标，之后将围绕这个点缩放
        mCenterXOnGraffiti = mGraffitiView.toX(mGraffitiView.getWidth() / 2);
        mCenterYOnGraffiti = mGraffitiView.toY(mGraffitiView.getHeight() / 2);

        if (v.getId() == R.id.btn_amplifier) { // 放大
            ThreadUtil.getInstance().runOnAsyncThread(new Runnable() {
                public void run() {
                    do {
                        mScale += 0.05f;
                        if (mScale > mMaxScale) {
                            mScale = mMaxScale;
                            mIsScaling = false;
                        }
                        updateScale();
                        try {
                            Thread.sleep(TIME_SPAN);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while (mIsScaling);

                }
            });
        } else if (v.getId() == R.id.btn_reduce) { // 缩小
            ThreadUtil.getInstance().runOnAsyncThread(new Runnable() {
                public void run() {
                    do {
                        mScale -= 0.05f;
                        if (mScale < mMinScale) {
                            mScale = mMinScale;
                            mIsScaling = false;
                        }
                        updateScale();
                        try {
                            Thread.sleep(TIME_SPAN);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while (mIsScaling);
                }
            });
        }
    }

    private void updateScale() {
        if (mUpdateScale == null) {

            mUpdateScale = new Runnable() {
                public void run() {
                    // 围绕坐标(0,0)缩放图片
                    mGraffitiView.setScale(mScale);
                    // 缩放后，偏移图片，以产生围绕某个点缩放的效果
                    float transX = mGraffitiView.toTransX(mGraffitiView.getWidth() / 2, mCenterXOnGraffiti);
                    float transY = mGraffitiView.toTransY(mGraffitiView.getHeight() / 2, mCenterYOnGraffiti);
                    mGraffitiView.setTrans(transX, transY);
                }
            };
        }
        ThreadUtil.getInstance().runOnMainThread(mUpdateScale);
    }

    private void showView(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }

        view.clearAnimation();
        view.startAnimation(mViewShowAnimation);
        view.setVisibility(View.VISIBLE);
        if (view == mSettingsPanel || mBtnHidePanel.isSelected()) {
            mGraffitiView.setAmplifierScale(-1);
        }
    }

    private void hideView(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            if (view == mSettingsPanel && mGraffitiView.getAmplifierScale() > 0) {
                mGraffitiView.setAmplifierScale(-1);
            }
            return;
        }
        view.clearAnimation();
        view.startAnimation(mViewHideAnimation);
        view.setVisibility(View.GONE);
        if (view == mSettingsPanel && !mBtnHidePanel.isSelected() && !mBtnMovePic.isSelected()) {
            // 当设置面板隐藏时才显示放大器
            mGraffitiView.setAmplifierScale(mGraffitiParams.mAmplifierScale);
        } else if ((view == mSettingsPanel && mGraffitiView.getAmplifierScale() > 0)) {
            mGraffitiView.setAmplifierScale(-1);
        }
    }

    /**
     * 设置底部显示
     */
    public void setButtonMenuShow(View view, boolean isSelfe, boolean show, GraffitiView.Pen pen) {


        if (pen == GraffitiView.Pen.HAND) {//手绘

            //删除按钮
            rd_delete.setVisibility(View.GONE);
            //显示撤销
            ll_back.setVisibility(View.VISIBLE);
            //设置撤销match
            ViewGroup.LayoutParams layoutParams = ll_back.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            ll_back.setLayoutParams(layoutParams);
            //颜色地板显示
            rg_bottom.setVisibility(View.VISIBLE);

            afterClick(view, isSelfe, show);

            viewClickShow(pen);
        } else if (pen == GraffitiView.Pen.MOSAIC) {//马赛克

            //删除按钮
            rd_delete.setVisibility(View.GONE);
            //显示撤销
            ll_back.setVisibility(View.VISIBLE);
            //设置撤销match
            ViewGroup.LayoutParams layoutParams = ll_back.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            ll_back.setLayoutParams(layoutParams);

            //颜色地板显示
            rg_bottom.setVisibility(View.GONE);
            //线
            tv_color_line.setVisibility(View.GONE);

            afterClick(view, isSelfe, show);
            viewClickShow(pen);
        } else if (pen == GraffitiView.Pen.TEXT) {//文本

            //删除按钮
            boolean textShow = mGraffitiView.getSelectedItem() != null;
            rd_delete.setVisibility(textShow ? View.VISIBLE : View.GONE);
            //显示撤销
            ll_back.setVisibility(View.GONE);
            //颜色地板显示
            rg_bottom.setVisibility(View.GONE);

            afterClick(view, isSelfe, show);

//            ll_button_color.setVisibility(View.GONE);

            viewClickShow(pen);
        } else if (pen == GraffitiView.Pen.SCREEN) {//截屏

            ll_button_color.setVisibility(View.GONE);
            viewClickShow(pen);
        }


        //颜色菜单
//        ll_button_color= (LinearLayout) findViewById(R.id.ll_button_color);
        //撤销按钮
//        ll_back= (LinearLayout) findViewById(R.id.ll_back);
        //颜色按钮集合
//        rg_bottom= (LinearLayout) findViewById(R.id.rg_bottom);
        //颜色和撤销之间的线
//        tv_color_line= (TextView) findViewById(R.id.tv_color_line);
    }

    /**
     * 点击之后的设置
     */
    public void afterClick(View view, boolean isSelfe, boolean show) {
        boolean oldLLAtate=ll_button_color.getVisibility()==View.VISIBLE;
        if (isSelfe) {
            ll_button_color.setVisibility(oldLLAtate  ? View.GONE : View.VISIBLE);
        } else {
            ll_button_color.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        boolean newLLAtate=ll_button_color.getVisibility()==View.VISIBLE;
        mIsMovingPic = !newLLAtate;

        try {
            LogUtil.e("state-viewState===" + newLLAtate);
            ((IconFontRadioButton) view).setChecked(newLLAtate);
            view.setSelected(newLLAtate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 各个按钮点击事件
     */
    public void viewClickShow(GraffitiView.Pen pen) {

        boolean viewState = ll_button_color.getVisibility() == View.VISIBLE;

        if (pen == GraffitiView.Pen.HAND) {//手绘
            rd_write.setTextColor(viewState ? this.getResources().getColor(R.color.selected_color) :
                    this.getResources().getColor(R.color.white));
            rd_text.setTextColor(this.getResources().getColor(R.color.white));
            rd_mosaic.setTextColor(this.getResources().getColor(R.color.white));
            rd_screen.setTextColor(this.getResources().getColor(R.color.white));
        } else if (pen == GraffitiView.Pen.TEXT) {//文字
            rd_write.setTextColor(this.getResources().getColor(R.color.white));
            rd_text.setTextColor(viewState ? this.getResources().getColor(R.color.selected_color) :
                    this.getResources().getColor(R.color.white));
            rd_mosaic.setTextColor(this.getResources().getColor(R.color.white));
            rd_screen.setTextColor(this.getResources().getColor(R.color.white));
        } else if (pen == GraffitiView.Pen.MOSAIC) {//马赛克
            rd_write.setTextColor(this.getResources().getColor(R.color.white));
            rd_text.setTextColor(this.getResources().getColor(R.color.white));
            rd_mosaic.setTextColor(viewState ? this.getResources().getColor(R.color.selected_color) :
                    this.getResources().getColor(R.color.white));
            rd_screen.setTextColor(this.getResources().getColor(R.color.white));
        } else if (pen == GraffitiView.Pen.SCREEN) {//截屏
            rd_write.setTextColor(this.getResources().getColor(R.color.white));
            rd_text.setTextColor(this.getResources().getColor(R.color.white));
            rd_mosaic.setTextColor(this.getResources().getColor(R.color.white));
            rd_screen.setTextColor(viewState ? this.getResources().getColor(R.color.selected_color) :
                    this.getResources().getColor(R.color.white));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cropImageView != null) {
//            cropImageView.setOnSetImageUriCompleteListener(null);
            cropImageView.setOnCropImageCompleteListener(null);
        }
    }

}
