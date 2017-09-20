package cn.hzw.graffiti.edit_image;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cn.forward.androids.utils.LogUtil;
import cn.hzw.graffiti.R;


/**
 * Created by showzeng on 17-8-11.
 * Email: kingstageshow@gmail.com
 * Github: https://github.com/showzeng
 */

public class AddTextDialogFragment extends DialogFragment implements View.OnClickListener {

    private Dialog mDialog;
    private EditText commentEditText;
    private InputMethodManager inputMethodManager;
    private DialogFragmentDataCallback dataCallback;
    private ColorOnCLickListener onCLickListener;

    private TextView tv_cancel, tv_ensure;
    private ImageView red_image, origin_image, green_image, blue_image, white_image, black_image;
    private LinearLayout rg_bottom_group,ll_back;

    private Map<String, Object> map;
    private int color = R.color.red;
    private String context = "";

    public static AddTextDialogFragment newInstance(Map<String, Object> map) {
        AddTextDialogFragment dialog = new AddTextDialogFragment();

        Bundle args = new Bundle();
        args.putSerializable("map", (Serializable) map);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onAttach(Activity context) {
        if (!(context instanceof DialogFragmentDataCallback)) {
            throw new IllegalStateException("DialogFragment 所在的 activity 必须实现 DialogFragmentDataCallback 接口");
        }
        super.onAttach(context);
        dataCallback = (DialogFragmentDataCallback) context;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        try {
            map = (Map<String, Object>) getArguments().getSerializable("map");
            context = (String) map.get(AppParmers.TEXT_CONTENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
//            color = (int) map.get(AppParmers.TEXT_COLOR);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mDialog = new Dialog(getActivity(), R.style.alertDialogView);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.dialog_fragment_add_text);
        mDialog.setCanceledOnTouchOutside(true);

        Window window = mDialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
//        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);

        //init view
        commentEditText = (EditText) mDialog.findViewById(R.id.et_add_text);
        tv_cancel = (TextView) mDialog.findViewById(R.id.tv_cancel);
        tv_ensure = (TextView) mDialog.findViewById(R.id.tv_ensure);
        //菜单
        rg_bottom_group = (LinearLayout) mDialog.findViewById(R.id.rg_bottom_group);
        rg_bottom_group.setVisibility(View.GONE);
        //颜色
        red_image = (ImageView) mDialog.findViewById(R.id.red_image);
        origin_image = (ImageView) mDialog.findViewById(R.id.origin_image);
        blue_image = (ImageView) mDialog.findViewById(R.id.blue_image);
        green_image = (ImageView) mDialog.findViewById(R.id.green_image);
        white_image = (ImageView) mDialog.findViewById(R.id.white_image);
        black_image = (ImageView) mDialog.findViewById(R.id.black_image);

        ll_back= (LinearLayout) mDialog.findViewById(R.id.ll_back);

        ll_back.setVisibility(View.GONE);

        //初始化文字
        if (!TextUtils.isEmpty(context)) {
            commentEditText.setText(context);
        }

        setSoftKeyboard();

        onCLickListener = new ColorOnCLickListener();

        //init lisener
        tv_ensure.setOnClickListener(this);
        tv_cancel.setOnClickListener(this);
        //颜色事件
        red_image.setOnClickListener(onCLickListener);
        origin_image.setOnClickListener(onCLickListener);
        green_image.setOnClickListener(onCLickListener);
        blue_image.setOnClickListener(onCLickListener);
        white_image.setOnClickListener(onCLickListener);
        black_image.setOnClickListener(onCLickListener);

//        switch (color) {
//            case R.color.red:
//                red_image.performClick();
//                break;
//            case R.color.orange:
//                origin_image.performClick();
//                break;
//            case R.color.green:
//                green_image.performClick();
//                break;
//            case R.color.blue:
//                blue_image.performClick();
//                break;
//            case R.color.white:
//                white_image.performClick();
//                break;
//            case R.color.black:
//                black_image.performClick();
//                break;
//            default:
//                //默认是红色
//                red_image.performClick();
//                break;
//        }
        //默认是红色
        red_image.performClick();
        //初始化颜色
        setTextColor(color);

        return mDialog;
    }

    private void setSoftKeyboard() {
        commentEditText.setFocusable(true);
        commentEditText.setFocusableInTouchMode(true);
        commentEditText.requestFocus();

        // TODO: 17-8-11 为何这里要延时才能弹出软键盘, 延时时长又如何判断？ 目前是手动调试
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
            }
        }, 150);
    }

    private class ColorOnCLickListener implements View.OnClickListener {
        private View mLastColoriew;

        @Override
        public void onClick(View v) {

            if (v.getId() == R.id.red_image) {//颜色
                setTextColor(R.color.red);
                color = R.color.red;
            } else if (v.getId() == R.id.origin_image) {
                setTextColor(R.color.orange);
                color = R.color.orange;
            } else if (v.getId() == R.id.green_image) {
                setTextColor(R.color.green);
                color = R.color.green;
            } else if (v.getId() == R.id.blue_image) {
                setTextColor(R.color.blue);
                color = R.color.blue;
            } else if (v.getId() == R.id.white_image) {
                setTextColor(R.color.white);
                color = R.color.white;
            } else if (v.getId() == R.id.black_image) {
                setTextColor(R.color.black);
                color = R.color.black;
            }
            if (mLastColoriew != null) {
                mLastColoriew.setSelected(false);
            }
            v.setSelected(true);
            mLastColoriew = v;

        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.tv_ensure){//确定
            if (dataCallback!= null) {
                if (map == null) {
                    map = new HashMap<>();
                }
                map.put(AppParmers.TEXT_CONTENT, commentEditText.getText().toString());
                map.put(AppParmers.TEXT_COLOR, color);

                Log.e("text-","-start===x==="+commentEditText.getText().toString());

                dataCallback.setCommentText(map);
            }else{
                Log.e("text-","-start-为空===x==="+commentEditText.getText().toString());
            }
            if (mDialog != null) {
                mDialog.dismiss();
            }
        }else if(v.getId()==R.id.tv_cancel){//取消
            if (mDialog != null) {
                mDialog.dismiss();
            }
        }else if(v.getId()==R.id.red_image){
            setTextColor(R.color.red);
        }else if(v.getId()==R.id.origin_image){
            setTextColor(R.color.orange);
        }else if(v.getId()==R.id.green_image){
            setTextColor(R.color.green);
        }else if(v.getId()==R.id.blue_image){
            setTextColor(R.color.blue);
        }else if(v.getId()==R.id.white_image){
            setTextColor(R.color.white);
        }else if(v.getId()==R.id.black_image){
            setTextColor(R.color.black);
        }

    }

    public void setTextColor(int color) {
        commentEditText.setTextColor(this.getResources().getColor(color));
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
//        dataCallback.setCommentText(commentEditText.getText().toString());
        super.onDismiss(dialog);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
//        dataCallback.setCommentText(commentEditText.getText().toString());
        super.onCancel(dialog);
    }
}
