package com.example.baitaplon.models;

public class QuestionTag {
    private String questionId;
    private String tagId;

    public QuestionTag() {}

    public QuestionTag(String questionId, String tagId) {
        this.questionId = questionId;
        this.tagId = tagId;
    }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }
}
