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

    /**
     * 자연수를 요일
     * @param number 1~7
     * @return 문자 요일
     */
    public static String dayOfString(int number) {
        switch (number) {
            case 1:
                return "일";
            case 2:
                return "월";
            case 3:
                return "화";
            case 4:
                return "수";
            case 5:
                return "목";
            case 6:
                return "금";
            case 7:
                return "토";
            default:
                return "알수 없음";
        }
    }
}
