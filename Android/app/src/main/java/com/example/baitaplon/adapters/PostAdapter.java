package com.example.baitaplon.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.baitaplon.R;
import com.example.baitaplon.TimeUtils;
import com.example.baitaplon.models.Question;
import com.example.baitaplon.models.Tag;
import com.example.baitaplon.models.User;
import com.example.baitaplon.models.QuestionTag;
import com.example.baitaplon.PostDetailActivity;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Question> questionList;
    private Context context;
    private DatabaseReference usersRef, tagsRef, questionTagsRef, questionsRef;

    public PostAdapter(Context context, List<Question> questions) {
        this.context = context;
        this.questionList = questions;
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        usersRef = db.getReference("users");
        tagsRef = db.getReference("tags");
        questionTagsRef = db.getReference("questionTags");
        questionsRef = db.getReference("questions");
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivLike, ivDislike, ivComment;
        TextView tvAuthorName, tvReputation, tvPostTime, tvTitle, tvTags;
        TextView tvLikeCount, tvCommentCount;

        public PostViewHolder(View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.iv_author_avatar);
            tvAuthorName = v.findViewById(R.id.tv_author_name);
            tvReputation = v.findViewById(R.id.tv_reputation_score);
            tvPostTime = v.findViewById(R.id.tv_post_time);
            tvTitle = v.findViewById(R.id.tv_post_title);
            tvTags = v.findViewById(R.id.tv_post_tags);
            tvLikeCount = v.findViewById(R.id.tv_like_count);
            tvCommentCount = v.findViewById(R.id.tv_comment_count);
            ivLike = v.findViewById(R.id.iv_like_icon);
            ivDislike = v.findViewById(R.id.iv_dislike_icon);
            ivComment = v.findViewById(R.id.iv_comment_icon);
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int pos) {
        Question q = questionList.get(pos);
        holder.tvTitle.setText(q.getTitle());
        holder.tvPostTime.setText(TimeUtils.getRelativeTime(q.getCreatedAt()));
        holder.tvLikeCount.setText(String.valueOf(q.getVoteCount()));
        holder.tvCommentCount.setText(String.valueOf(q.getComment()));

        usersRef.child(q.getAuthorUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot usnap) {
                User user = usnap.getValue(User.class);
                if (user != null) {
                    holder.tvAuthorName.setText(user.getFullName());
                    holder.tvReputation.setText(user.getReputationScore() + " điểm");

                    String avatarUrl = user.getAvatar();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(context)
                                .load(avatarUrl)
                                .placeholder(R.drawable.anhdaidien)
                                .error(R.drawable.anhdaidien)
                                .circleCrop()
                                .into(holder.ivAvatar);
                    } else {
                        holder.ivAvatar.setImageResource(R.drawable.anhdaidien);
                    }
                }
            }

            @Override public void onCancelled(DatabaseError e) {}
        });

        questionTagsRef.orderByChild("questionId").equalTo(q.getQuestionId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapQT) {
                        List<String> tagNames = new ArrayList<>();
                        for (DataSnapshot qtSnap : snapQT.getChildren()) {
                            QuestionTag qt = qtSnap.getValue(QuestionTag.class);
                            if (qt != null) {
                                String tagId = qt.getTagId();
                                tagsRef.child(tagId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override public void onDataChange(DataSnapshot tsnap) {
                                        Tag tag = tsnap.getValue(Tag.class);
                                        if (tag != null) {
                                            tagNames.add("#" + tag.getTagName());
                                            holder.tvTags.setText(String.join(" ", tagNames));
                                        }
                                    }

                                    @Override public void onCancelled(DatabaseError e) {}
                                });
                            }
                        }
                    }

                    @Override public void onCancelled(DatabaseError e) {}
                });

        // ✅ Mở chi tiết bài viết khi nhấn vào bài viết hoặc biểu tượng bình luận
        holder.itemView.setOnClickListener(v -> openPostDetail(q.getQuestionId()));
        holder.ivComment.setOnClickListener(v -> openPostDetail(q.getQuestionId()));


    }

    private void openPostDetail(String questionId) {
        Intent intent = new Intent(context, PostDetailActivity.class);
        intent.putExtra("questionId", questionId);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }
}
