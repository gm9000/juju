package com.juju.app.view.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import com.juju.app.R;


public class WarnTipDialog extends BaseDialog implements
		android.view.View.OnClickListener {
	private TextView btn_cancel, btn_ok;
	private  TextView mHtvText;
	private String mText;
	private static DialogInterface.OnClickListener mOnClickListener;
	private static DialogInterface.OnClickListener mOnCancleClickListener;
	private static BaseDialog mBaseDialog;// 当前的对话框

	public WarnTipDialog(Context context, String text) {
		super(context);
		mText = text;
		mBaseDialog = new BaseDialog(context);
		init();
	}

	private void init() {
		setContentView(R.layout.layout_dialog_warntip);
		mHtvText = (TextView) findViewById(R.id.dialog_generic_htv_message);
		mHtvText.setText(mText);
		btn_cancel = (TextView) findViewById(R.id.btn_cancel);
		btn_ok = (TextView) findViewById(R.id.btn_ok);
		btn_cancel.setOnClickListener(this);
		btn_ok.setOnClickListener(this);
	}

	public void setText(String text) {
		if (text == null) {
			mHtvText.setVisibility(View.GONE);
		} else {
			mText = text;
			mHtvText.setText(mText);
		}
	}

	public void setOkLable(String okLable){
		btn_ok.setText(okLable);
	}

	public void setCancleLable(String cancleLable){
		btn_cancel.setText(cancleLable);
	}

	public void setBtnOkLinstener(DialogInterface.OnClickListener listener) {
		mOnClickListener = listener;
	}

	public void setBtnCancelLinstener(DialogInterface.OnClickListener listener){
		mOnCancleClickListener = listener;
	}

	@Override
	public void dismiss() {
		if (isShowing()) {
			super.dismiss();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_cancel:
			if (isShowing()) {
				super.dismiss();
			}
			if (mOnCancleClickListener != null) {
				mOnCancleClickListener.onClick(mBaseDialog, 0);
			}
			break;

		case R.id.btn_ok:
			if (mOnClickListener != null) {
				mOnClickListener.onClick(mBaseDialog, 1);
			}
			if (isShowing()) {
				super.dismiss();
			}
			break;

		}
	}
}
