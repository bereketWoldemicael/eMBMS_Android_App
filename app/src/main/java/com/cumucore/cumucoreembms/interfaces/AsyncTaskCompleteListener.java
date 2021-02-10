package com.cumucore.cumucoreembms.interfaces;


import com.expway.msp.EServiceType;

public interface AsyncTaskCompleteListener <T>{
    public void onTaskComplete(T result, EServiceType type);
}
