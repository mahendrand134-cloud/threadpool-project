package com.threadpool;

public enum BackpressureDecision {
    ACCEPT,
    THROTTLE,
    REJECT
}
