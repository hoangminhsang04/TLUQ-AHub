package com.example.baitaplon.models;

public class Vote {
    private String voteId;
    private String userId;
    private String questionId;
    private String answerId;
    private int voteType;

    public Vote() {}

    public Vote(String voteId, String userId, String questionId, String answerId, int voteType) {
        this.voteId = voteId;
        this.userId = userId;
        this.questionId = questionId;
        this.answerId = answerId;
        this.voteType = voteType;
    }

    public String getVoteId() { return voteId; }
    public void setVoteId(String voteId) { this.voteId = voteId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getAnswerId() { return answerId; }
    public void setAnswerId(String answerId) { this.answerId = answerId; }

    public int getVoteType() { return voteType; }
    public void setVoteType(int voteType) { this.voteType = voteType; }
}
