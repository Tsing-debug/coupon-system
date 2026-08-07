package com.ali.coupon.dto;

public class BatchUploadResult {

    private boolean success;
    private String jobId;
    private int totalCount;
    private int deduplicatedCount;
    private int newCount;
    private String message;

    private BatchUploadResult() {}

    public static BatchUploadResult success(String jobId, int totalCount, int deduplicatedCount, int newCount) {
        BatchUploadResult r = new BatchUploadResult();
        r.success = true;
        r.jobId = jobId;
        r.totalCount = totalCount;
        r.deduplicatedCount = deduplicatedCount;
        r.newCount = newCount;
        r.message = "任务已提交，新增 " + newCount + " 条，去重 " + deduplicatedCount + " 条";
        return r;
    }

    public static BatchUploadResult allDeduplicated(int totalCount) {
        BatchUploadResult r = new BatchUploadResult();
        r.success = true;
        r.totalCount = totalCount;
        r.deduplicatedCount = totalCount;
        r.newCount = 0;
        r.message = "全部已处理，无新增数据";
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getDeduplicatedCount() { return deduplicatedCount; }
    public void setDeduplicatedCount(int deduplicatedCount) { this.deduplicatedCount = deduplicatedCount; }
    public int getNewCount() { return newCount; }
    public void setNewCount(int newCount) { this.newCount = newCount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
