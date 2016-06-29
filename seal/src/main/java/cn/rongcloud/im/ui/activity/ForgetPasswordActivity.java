package cn.rongcloud.im.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import cn.rongcloud.im.R;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.response.CheckPhoneResponse;
import cn.rongcloud.im.server.response.RestPasswordResponse;
import cn.rongcloud.im.server.response.SendCodeResponse;
import cn.rongcloud.im.server.response.VerifyCodeResponse;
import cn.rongcloud.im.server.utils.AMUtils;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.utils.downtime.DownTimer;
import cn.rongcloud.im.server.utils.downtime.DownTimerListener;
import cn.rongcloud.im.server.widget.ClearWriteEditText;
import cn.rongcloud.im.server.widget.LoadDialog;

/**
 * Created by AMing on 16/2/2.
 * Company RongCloud
 */
public class ForgetPasswordActivity extends BaseActivity implements View.OnClickListener, DownTimerListener {

    private static final int CHECKPHONE = 31;
    private static final int SENDCODE = 32;
    private static final int CHANGEPASSWORD = 33;
    private static final int VERIFYCODE = 34;
    private static final int CHANGEPASSWORD_BACK = 1002;
    private ClearWriteEditText mPhone, mCode, mPassword1, mPassword2;

    private Button mGetCode, mOK;

    private String phone, mCodeToken;

    private boolean available;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget);
        ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.de_actionbar_back);
        actionBar.setTitle(R.string.forget_password);
        initView();

    }

    private void initView() {
        mPhone = (ClearWriteEditText) findViewById(R.id.forget_phone);
        mCode = (ClearWriteEditText) findViewById(R.id.forget_code);
        mPassword1 = (ClearWriteEditText) findViewById(R.id.forget_password);
        mPassword2 = (ClearWriteEditText) findViewById(R.id.forget_password1);
        mGetCode = (Button) findViewById(R.id.forget_getcode);
        mOK = (Button) findViewById(R.id.forget_button);
        mGetCode.setOnClickListener(this);
        mOK.setOnClickListener(this);
        mPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 11) {
                    if (AMUtils.isMobile(s.toString().trim())) {
                        phone = mPhone.getText().toString().trim();
                        request(CHECKPHONE, true);
                        AMUtils.onInactive(mContext, mPhone);
                    } else {
                        Toast.makeText(mContext, R.string.Illegal_phone_number, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    mGetCode.setClickable(false);
                    mGetCode.setBackgroundDrawable(getResources().getDrawable(R.drawable.rs_select_btn_gray));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 6) {
                    AMUtils.onInactive(mContext, mCode);
                    if (available) {
                        mOK.setClickable(true);
                        mOK.setBackgroundDrawable(getResources().getDrawable(R.drawable.rs_select_btn_blue));
                    }
                } else {
                    mOK.setClickable(false);
                    mOK.setBackgroundDrawable(getResources().getDrawable(R.drawable.rs_select_btn_gray));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    public Object doInBackground(int requestCode, String id) throws HttpException {
        switch (requestCode) {
            case CHECKPHONE:
                return action.checkPhoneAvailable("86", phone);
            case SENDCODE:
                return action.sendCode("86", phone);
            case CHANGEPASSWORD:
                return action.restPassword(mPassword1.getText().toString(), mCodeToken);
            case VERIFYCODE:
                return action.verifyCode("86", phone, mCode.getText().toString());
        }
        return super.doInBackground(requestCode, id);
    }

    @Override
    public void onSuccess(int requestCode, Object result) {
        if (result != null) {
            switch (requestCode) {
                case CHECKPHONE:
                    CheckPhoneResponse response = (CheckPhoneResponse) result;
                    if (response.getCode() == 200) {
                        if (response.isResult() == true) {
                            NToast.shortToast(mContext, getString(R.string.phone_unregister));
                            mGetCode.setClickable(false);
                            mGetCode.setBackgroundDrawable(getResources().getDrawable(R.drawable.rs_select_btn_gray));
                        } else {
                            available = true;
                            mGetCode.setClickable(true);
                            mGetCode.setBackgroundDrawable(getResources().getDrawable(R.drawable.rs_select_btn_blue));
                        }
                    }
                    break;
                case SENDCODE:
                    SendCodeResponse scrres = (SendCodeResponse) result;
                    if (scrres.getCode() == 200) {
                        NToast.shortToast(mContext, R.string.messge_send);
                    } else if (scrres.getCode() == 5000) {
                        NToast.shortToast(mContext, R.string.message_frequency);
                    }
                    break;
                case VERIFYCODE:
                    VerifyCodeResponse vcres = (VerifyCodeResponse) result;
                    switch (vcres.getCode()) {
                        case 200:
                            mCodeToken = vcres.getResult().getVerification_token();
                            if (!TextUtils.isEmpty(mCodeToken)) {
                                request(CHANGEPASSWORD);
                            } else {
                                NToast.shortToast(mContext, "code token is null");
                                LoadDialog.dismiss(mContext);
                            }
                            break;
                        case 1000:
                            //验证码错误
                            NToast.shortToast(mContext, R.string.verification_code_error);
                            LoadDialog.dismiss(mContext);
                            break;
                        case 2000:
                            //验证码过期
                            NToast.shortToast(mContext, R.string.captcha_overdue);
                            LoadDialog.dismiss(mContext);
                            break;
                    }
                    break;

                case CHANGEPASSWORD:
                    RestPasswordResponse response1 = (RestPasswordResponse) result;
                    if (response1.getCode() == 200) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, getString(R.string.update_success));
                        Intent data = new Intent();
                        data.putExtra("phone", phone);
                        data.putExtra("password", mPassword1.getText().toString());
                        setResult(CHANGEPASSWORD_BACK, data);
                        this.finish();
                    }
                    break;
            }
        }
    }

    @Override
    public void onFailure(int requestCode, int state, Object result) {
        switch (requestCode) {
            case CHECKPHONE:
                Toast.makeText(mContext, "手机号可用请求失败", Toast.LENGTH_SHORT).show();
                break;
            case SENDCODE:
                NToast.shortToast(mContext, "获取验证码请求失败");
                break;
        }
    }

    private DownTimer downTimer;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.forget_getcode:
                if (TextUtils.isEmpty(mPhone.getText().toString().trim())) {
                    NToast.longToast(mContext, getString(R.string.phone_number_is_null));
                } else {
                    downTimer = new DownTimer();
                    downTimer.setListener(this);
                    downTimer.startDown(60 * 1000);
                    request(SENDCODE);
                }
                break;
            case R.id.forget_button:
                if (TextUtils.isEmpty(mPhone.getText().toString())) {
                    NToast.shortToast(mContext, getString(R.string.phone_number_is_null));
                    mPhone.setShakeAnimation();
                    return;
                }

                if (TextUtils.isEmpty(mCode.getText().toString())) {
                    NToast.shortToast(mContext, getString(R.string.code_is_null));
                    mCode.setShakeAnimation();
                    return;
                }

                if (TextUtils.isEmpty(mPassword1.getText().toString())) {
                    NToast.shortToast(mContext, getString(R.string.password_is_null));
                    mPassword1.setShakeAnimation();
                    return;
                }

                if (TextUtils.isEmpty(mPassword2.getText().toString())) {
                    NToast.shortToast(mContext, getString(R.string.confirm_password));
                    mPassword2.setShakeAnimation();
                    return;
                }

                if (!mPassword2.getText().toString().equals(mPassword1.getText().toString())) {
                    NToast.shortToast(mContext, getString(R.string.passwords_do_not_match));
                    return;
                }

                LoadDialog.show(mContext);
                request(VERIFYCODE);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onTick(long millisUntilFinished) {
        mGetCode.setText("seconds:" + String.valueOf(millisUntilFinished / 1000));
        mGetCode.setClickable(false);
        mGetCode.setBackgroundDrawable(getResources().getDrawable(R.drawable.rs_select_btn_gray));
    }

    @Override
    public void onFinish() {
        mGetCode.setText(R.string.get_code);
        mGetCode.setClickable(true);
        mGetCode.setBackgroundDrawable(getResources().getDrawable(R.drawable.rs_select_btn_blue));
    }
}
