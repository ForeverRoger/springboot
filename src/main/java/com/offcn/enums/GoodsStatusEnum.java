package com.offcn.enums;

//测试git
public enum GoodsStatusEnum {

    GOODS_INIT_STATUS("0","初始状态"),
    GOODS_VERIFIED("1","审核通过"),
    GOODS_RREJECTED("2","未通过");

    private String status;
    private String statusMessage;

    GoodsStatusEnum(String status, String statusMessage) {
        this.status = status;
        this.statusMessage = statusMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
