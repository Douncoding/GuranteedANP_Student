package com.douncoding.guaranteedanp_s;

import android.text.TextUtils;
import android.widget.EditText;

import com.douncoding.dao.Track;

/**
 * 기타 공통적인 UI 서비스를 정의
 *
 */
public class CommonUtils {

    /**
     * EditText 의 값이 입력되지 않은 경우에 대한 에러처리
     * @param editText 대상
     * @param errMessage 에러 메시지
     * @return EditText 에 입력된 문자열
     */
    public static String getStringAndEmptyErrorHandle(EditText editText, String errMessage) {
        String str = editText.getText().toString();

        if (TextUtils.isEmpty(str)) {
            if (errMessage != null)
                editText.setError(errMessage);
            else
                editText.setError("");
        } else {
            editText.setError(null);
        }

        return str;
    }

    public static String getStringAndEmptyErrorHandle(EditText editText) {
        return getStringAndEmptyErrorHandle(editText, null);
    }
}
