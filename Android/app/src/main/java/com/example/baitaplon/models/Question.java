package com.example.baitaplon.models;

public class Question {
    private String questionId;
    private String title;
    private String content;
    private String authorUserId;
    private String createdAt;
    private int voteCount;
    private int comment;
    private String acceptedAnswerId;
    private String status;

    public Question() {}

    public Question(String questionId, String title, String content, String authorUserId,
                    String createdAt, int voteCount, int comment, String acceptedAnswerId, String status) {
        this.questionId = questionId;
        this.title = title;
        this.content = content;
        this.authorUserId = authorUserId;
        this.createdAt = createdAt;
        this.voteCount = voteCount;
        this.comment = comment;
        this.acceptedAnswerId = acceptedAnswerId;
        this.status = status;
    }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(String authorUserId) { this.authorUserId = authorUserId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }

    public int getComment() { return comment; }
    public void setComment(int comment) { this.comment = comment; }

    public String getAcceptedAnswerId() { return acceptedAnswerId; }
    public void setAcceptedAnswerId(String acceptedAnswerId) { this.acceptedAnswerId = acceptedAnswerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
