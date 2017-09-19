package image.edit.gjj;

import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import cn.forward.androids.utils.LogUtil;
import cn.hzw.graffiti.GraffitiActivity;
import cn.hzw.imageselector.ImageLoader;
import cn.hzw.imageselector.ImageSelectorActivity;
import cn.hzw.graffiti.edit_image.AppParmers;
import cn.hzw.graffiti.edit_image.EditImageActivity;

public class MainActivity extends AppCompatActivity {

    public static final int REQ_CODE_SELECT_IMAGE = 100;
    public static final int REQ_CODE_GRAFFITI = 101;
    private TextView mPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_select_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageSelectorActivity.startActivityForResult(REQ_CODE_SELECT_IMAGE, MainActivity.this, null, false);
            }
        });
        mPath = (TextView) findViewById(R.id.img_path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SELECT_IMAGE) {
            if (data == null) {
                return;
            }
            ArrayList<String> list = data.getStringArrayListExtra(ImageSelectorActivity.KEY_PATH_LIST);
            if (list != null && list.size() > 0) {
                LogUtil.e("Graffiti", list.get(0));

                String outPutPath=MainActivity.this.getFilesDir().getPath()+"/"+ SystemClock.currentThreadTimeMillis()+".png";
                LogUtil.e("path-outPutPath==="+outPutPath);

                EditImageActivity.start(MainActivity.this,list.get(0),outPutPath,false,"这是data",REQ_CODE_GRAFFITI);
            }
        } else if (requestCode == REQ_CODE_GRAFFITI) {
            if (data == null) {
                return;
            }
            data.getStringExtra(AppParmers.EDIT_FROM_DATA);

            Toast.makeText(getApplicationContext(),"data==="+data,Toast.LENGTH_SHORT).show();

            if (resultCode == GraffitiActivity.RESULT_OK) {
                String path = data.getStringExtra(GraffitiActivity.KEY_IMAGE_PATH);
                if (TextUtils.isEmpty(path)) {
                    return;
                }
                ImageLoader.getInstance(this).display(findViewById(R.id.img), path);
                mPath.setText(path);
            } else if (resultCode == GraffitiActivity.RESULT_ERROR) {
                Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

