package com.hust.tutu.redis;

public interface KeyPrefix {

    public int expireSeconds();

    public String getPrefix();
}
