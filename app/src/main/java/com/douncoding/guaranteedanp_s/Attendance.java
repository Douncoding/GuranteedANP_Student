package com.douncoding.guaranteedanp_s;

import java.util.Date;

public class Attendance {
    int id;
    int state;
    Date enterTime;
    Date exitTime;
    int eid; // Enrollment ID
    int ltid; // LessonTime ID

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Date getEnterTime() {
        return enterTime;
    }

    public void setEnterTime(Date enterTime) {
        this.enterTime = enterTime;
    }

    public Date getExitTime() {
        return exitTime;
    }

    public void setExitTime(Date exitTime) {
        this.exitTime = exitTime;
    }

    public int getEid() {
        return eid;
    }

    public void setEid(int eid) {
        this.eid = eid;
    }

    public int getLtid() {
        return ltid;
    }

    public void setLtid(int ltid) {
        this.ltid = ltid;
    }
}
