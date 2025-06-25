package com.example.baitaplon.models;

public class User {
    private String userId;
    private String email;
    private String passwordHash;
    private String fullName;
    private int reputationScore;
    private String avatar;
    private String createdAt;
    private String birthday;
    private String address;
    private String sex;
    private String roleId;

    public User() {}

    public User(String userId, String email, String passwordHash, String fullName, int reputationScore,
                String avatar, String createdAt, String birthday, String address, String sex, String roleId) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.reputationScore = reputationScore;
        this.avatar = avatar;
        this.createdAt = createdAt;
        this.birthday = birthday;
        this.address = address;
        this.sex = sex;
        this.roleId = roleId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public int getReputationScore() { return reputationScore; }
    public void setReputationScore(int reputationScore) { this.reputationScore = reputationScore; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

}
