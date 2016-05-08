package com.douncoding.guaranteedanp_s;

public class Constants {
    // 웹 서버 도메인
    // public static String HOST = "http://192.168.123.105:50100";
    public static String HOST = "http://192.168.0.122:50100";

    // 로컬 데이터베이스 이름
    public static final String DATABASE_NAME = "GuaranteedANPDB";

    // Preference 이름
    public static final String PREFERENCE_NAME = "GANP_PREFERENCE";

    // BEACON 상태 관리
    public static final String BEACON_BROADCAST_ACTION =
            "com.douncoding.guaranteedanp.BEACON_BROADCAST";

    public static final String DEVELOP_BROADCAST_ACTION =
            "com.douncoding.guaranteedanp.DEVELOP_BROADCAST";
    public static final String EXTRA_DEVLOP_OPTIONS = "beaconExtraOptions";
    public static final int OPTIONS_TEST = 1;

    public static final int SERVICE_FROGROUND_NOTI_ID = 2;
}
