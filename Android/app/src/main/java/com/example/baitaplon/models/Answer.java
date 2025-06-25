package com.example.baitaplon.models;

public class Answer {
    private String answerId;
    private String content;
    private String questionId;
    private String authorUserId;
    private String createdAt;
    private int voteCount;
    private boolean isAccepted;

    public Answer() {}

    public Answer(String answerId, String content, String questionId, String authorUserId,
                  String createdAt, int voteCount, boolean isAccepted) {
        this.answerId = answerId;
        this.content = content;
        this.questionId = questionId;
        this.authorUserId = authorUserId;
        this.createdAt = createdAt;
        this.voteCount = voteCount;
        this.isAccepted = isAccepted;
    }

    public String getAnswerId() { return answerId; }
    public void setAnswerId(String answerId) { this.answerId = answerId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(String authorUserId) { this.authorUserId = authorUserId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }

    public boolean isAccepted() { return isAccepted; }
    public void setAccepted(boolean accepted) { isAccepted = accepted; }
}
