package com.minbao.multiverse.manager;

public interface OssManager {
    String upload(byte[] data, String fileName);
    byte[] download(String url);
}
