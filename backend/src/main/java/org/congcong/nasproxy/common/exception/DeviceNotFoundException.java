package org.congcong.nasproxy.common.exception;

public class DeviceNotFoundException extends RuntimeException{

    public DeviceNotFoundException(String detail) {
        super(detail);
    }
}
