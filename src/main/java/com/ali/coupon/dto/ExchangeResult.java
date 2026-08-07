package com.ali.coupon.dto;

public class ExchangeResult {

    private boolean success;
    private int code;
    private String message;

    private ExchangeResult(boolean success, int code, String message) {
        this.success = success;
        this.code = code;
        this.message = message;
    }

    public static ExchangeResult success() {
        return new ExchangeResult(true, 1, "兑换成功");
    }

    public static ExchangeResult stockInsufficient() {
        return new ExchangeResult(false, -1, "库存不足");
    }

    public static ExchangeResult duplicate() {
        return new ExchangeResult(false, -2, "您已参与过该活动");
    }

    public static ExchangeResult fail(int code) {
        return new ExchangeResult(false, code, "兑换失败, code=" + code);
    }

    public boolean isSuccess() { return success; }
    public int getCode() { return code; }
    public String getMessage() { return message; }
}
