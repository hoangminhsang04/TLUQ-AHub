package com.example.baitaplon;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.baitaplon.adapters.AnswerAdapter;
import com.example.baitaplon.models.*;
import com.google.firebase.database.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bumptech.glide.Glide;

public class PostDetailActivity extends AppCompatActivity {
    private String questionId;
    private DatabaseReference db, votesRef, questionsRef;
    private TextView tvTitle, tvContent, tvTags, tvTime, tvAuthor, tvReputation;
    private TextView tvVoteCount, tvCommentCount;
    private ImageView ivAvatar, ivLike, ivUnlike;
    private RecyclerView rvAnswers;
    private List<Answer> answerList;
    private AnswerAdapter answerAdapter;
    private String currentUserId;
    private int currentVoteType = 0;
    private EditText etComment;
    private ImageButton btnSendComment;
    private Question post;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        questionId = getIntent().getStringExtra("questionId");
        if (questionId == null) {
            Toast.makeText(this, "Không tìm thấy ID bài viết", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PostDetailActivity.this, UserActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        db = FirebaseDatabase.getInstance().getReference();
        votesRef = db.child("votes");
        questionsRef = db.child("questions");
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", null);
        etComment = findViewById(R.id.et_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);
        mappingViews();
        setupListeners();
        loadPostDetails(questionId);
        setupAnswerRecyclerView();
        loadAnswerList(questionId);
        loadVoteStatus();
        btnSendComment.setOnClickListener(v -> {
            String content = etComment.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
                return;
            }

            sendAnswer(content);
        });
    }

    private void mappingViews() {
        tvTitle = findViewById(R.id.tvPostTitle);
        tvContent = findViewById(R.id.tvPostContent);
        tvTags = findViewById(R.id.tvPostTags);
        tvTime = findViewById(R.id.tvPostTime);
        tvAuthor = findViewById(R.id.tvAuthorName);
        tvReputation = findViewById(R.id.tvReputation);
        tvVoteCount = findViewById(R.id.tvLikeCount);
        tvCommentCount = findViewById(R.id.tvCommentCount);
        ivAvatar = findViewById(R.id.ivAuthorAvatar);
        ivLike = findViewById(R.id.ivLike);
        ivUnlike = findViewById(R.id.ivUnlike);
        rvAnswers = findViewById(R.id.rvAnswers);
    }
    private void setupListeners() {
        ivLike.setOnClickListener(v -> handleVote(1));
        ivUnlike.setOnClickListener(v -> handleVote(-1));
    }
    private void loadPostDetails(String id) {
        db.child("questions").child(id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Question q = snapshot.getValue(Question.class);
                if (q == null) return;
                post = q;

                tvTitle.setText(q.getTitle());
                tvContent.setText(q.getContent());
                tvTime.setText(TimeUtils.getRelativeTime(q.getCreatedAt()));
                tvVoteCount.setText(String.valueOf(q.getVoteCount()));
                tvCommentCount.setText(String.valueOf(q.getComment()));

                // Tải thông tin người dùng
                db.child("users").child(q.getAuthorUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        User user = snap.getValue(User.class);
                        if (user != null) {
                            tvAuthor.setText(user.getFullName());
                            tvReputation.setText(user.getReputationScore() + " điểm");
                            String avatarUrl = user.getAvatar();
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(PostDetailActivity.this)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.anhdaidien)
                                        .error(R.drawable.anhdaidien)
                                        .circleCrop()
                                        .into(ivAvatar);
                            } else {
                                ivAvatar.setImageResource(R.drawable.anhdaidien);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

                // Tải tag
                db.child("questionTags").orderByChild("questionId").equalTo(id)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapTags) {
                                StringBuilder sb = new StringBuilder();
                                for (DataSnapshot tagSnap : snapTags.getChildren()) {
                                    QuestionTag qt = tagSnap.getValue(QuestionTag.class);
                                    if (qt != null) {
                                        db.child("tags").child(qt.getTagId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapT) {
                                                Tag t = snapT.getValue(Tag.class);
                                                if (t != null) {
                                                    sb.append("#").append(t.getTagName()).append(" ");
                                                    tvTags.setText(sb.toString().trim());
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {}
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PostDetailActivity.this, "Lỗi tải bài viết", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setupAnswerRecyclerView() {
        answerList = new ArrayList<>();
        answerAdapter = new AnswerAdapter(this, answerList,
                new AnswerAdapter.OnVoteClearedListener() {
                    @Override
                    public void onQuestionVoteCleared() {
                        ivLike.setColorFilter(null);
                        ivUnlike.setColorFilter(null);
                    }
                },
                new AnswerAdapter.OnQuestionVoteCountChangedListener() {
                    @Override
                    public void onQuestionVoteCountChanged(int newCount) {
                        tvVoteCount.setText(String.valueOf(newCount));
                        if (post != null) post.setVoteCount(newCount);
                    }
                }
        );

        // ✅ Gắn listener để cập nhật điểm danh tiếng
        answerAdapter.setOnQuestionAuthorReputationChangedListener(new AnswerAdapter.OnQuestionAuthorReputationChangedListener() {
            @Override
            public void onQuestionAuthorReputationChanged(int newReputation) {
                tvReputation.setText(newReputation + " điểm");
            }
        });

        rvAnswers.setAdapter(answerAdapter);
        rvAnswers.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadAnswerList(String questionId) {
        db.child("answers").orderByChild("questionId").equalTo(questionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        answerList.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Answer answer = snap.getValue(Answer.class);
                            if (answer != null) {
                                answerList.add(answer);
                            }
                        }
                        answerAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(PostDetailActivity.this, "Lỗi tải danh sách câu trả lời", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void loadVoteStatus() {
        votesRef.orderByChild("questionId").equalTo(questionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Vote vote = snap.getValue(Vote.class);
                            if (vote != null && vote.getUserId().equals(currentUserId) && vote.getAnswerId() == null) {
                                currentVoteType = vote.getVoteType();
                                updateVoteUI(currentVoteType);
                                break;
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }
    private void handleVote(int voteType) {
        if (currentUserId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để vote", Toast.LENGTH_SHORT).show();
            return;
        }

        votesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> votesToRemove = new ArrayList<>();
                List<String> answerIdsToDecrement = new ArrayList<>();
                List<String> answerAuthorIdsToUpdate = new ArrayList<>();
                boolean hasVotedQuestion = false;
                String existingQuestionVoteId = null;
                int existingVoteType = 0;

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Vote vote = snap.getValue(Vote.class);
                    if (vote != null && vote.getUserId().equals(currentUserId)) {
                        if (vote.getAnswerId() != null) {
                            for (Answer a : answerList) {
                                if (vote.getAnswerId().equals(a.getAnswerId())) {
                                    votesToRemove.add(snap.getKey());
                                    answerIdsToDecrement.add(a.getAnswerId());
                                }
                            }
                        }

                        if (vote.getQuestionId() != null && vote.getQuestionId().equals(questionId) && vote.getAnswerId() == null) {
                            hasVotedQuestion = true;
                            existingQuestionVoteId = snap.getKey();
                            existingVoteType = vote.getVoteType();
                        }
                    }
                }

                // Nếu đã vote và cùng loại
                if (hasVotedQuestion && existingVoteType == voteType) {
                    Toast.makeText(PostDetailActivity.this, "Bạn đã vote bài viết này rồi", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Xoá vote câu trả lời nếu có
                for (int i = 0; i < votesToRemove.size(); i++) {
                    String voteId = votesToRemove.get(i);
                    String answerId = answerIdsToDecrement.get(i);

                    // Lấy authorUserId của câu trả lời để cập nhật điểm
                    db.child("answers").child(answerId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String authorUserId = snapshot.child("authorUserId").getValue(String.class);

                                // Xoá vote
                                votesRef.child(voteId).removeValue();

                                // Giảm voteCount
                                db.child("answers").child(answerId).child("voteCount")
                                        .runTransaction(new Transaction.Handler() {
                                            @NonNull
                                            @Override
                                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                                Integer count = currentData.getValue(Integer.class);
                                                if (count == null) count = 0;
                                                currentData.setValue(Math.max(count - 1, 0));
                                                return Transaction.success(currentData);
                                            }

                                            @Override
                                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                                                // ✅ Sau khi xóa vote → cập nhật lại điểm cho author
                                                if (authorUserId != null) {
                                                    updateReputationScore(authorUserId, null);
                                                }
                                            }
                                        });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Vote", "Lỗi khi lấy tác giả answer: " + error.getMessage());
                        }
                    });
                }


                // Nếu đã vote câu hỏi → cập nhật voteType
                if (hasVotedQuestion) {
                    votesRef.child(existingQuestionVoteId).child("voteType").setValue(voteType);
                } else {
                    // Chưa vote → thêm mới
                    String newId = votesRef.push().getKey();
                    Vote newVote = new Vote(newId, currentUserId, questionId, null, voteType);
                    votesRef.child(newId).setValue(newVote);
                    updateVoteCount(+1); // chỉ tăng khi là vote mới
                }

                // Lấy authorUserId để cập nhật reputationScore
                questionsRef.child(questionId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String authorUserId = snapshot.child("authorUserId").getValue(String.class);
                            if (authorUserId != null) {
                                updateReputationScore(authorUserId, new OnReputationUpdatedListener() {
                                    @Override
                                    public void onReputationUpdated(int reputation) {
                                        // Cập nhật giao diện nếu có TextView nào hiển thị điểm
                                        tvReputation.setText(String.valueOf(reputation)+" điểm");
                                    }
                                });
                            }
                        }
                        // Cập nhật UI và reload dữ liệu
                        updateVoteUI(voteType);
                        currentVoteType = voteType;
                        answerAdapter.clearUserVotes(currentUserId);
                        loadPostDetails(questionId);
                        loadAnswerList(questionId);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("handleVote", "Lỗi khi lấy authorUserId: " + error.getMessage());
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("handleVote", "Lỗi Firebase: " + error.getMessage());
            }
        });
    }
    private void updateVoteCount(int delta) {
        questionsRef.child(questionId).child("voteCount")
                .runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        Integer count = currentData.getValue(Integer.class);
                        if (count == null) count = 0;
                        currentData.setValue(count + delta);
                        return Transaction.success(currentData);
                    }
                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                        if (snapshot != null && snapshot.getValue() != null) {
                            tvVoteCount.setText(snapshot.getValue().toString());
                        }
                    }
                });
    }
    private void updateVoteUI(int voteType) {
        if (voteType == 1) {
            ivLike.setColorFilter(Color.BLUE);
            ivUnlike.setColorFilter(null);
        } else if (voteType == -1) {
            ivUnlike.setColorFilter(Color.RED);
            ivLike.setColorFilter(null);
        } else {
            ivLike.setColorFilter(null);
            ivUnlike.setColorFilter(null);
        }
    }
    private void sendAnswer(String content) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        DatabaseReference answersRef = db.child("answers");
        DatabaseReference questionsRef = db.child("questions").child(questionId);

        String newAnswerId = answersRef.push().getKey(); // Tạo ID mới
        String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .format(new java.util.Date());

        Answer newAnswer = new Answer(
                newAnswerId,
                content,
                questionId,
                currentUserId,
                currentTime,
                0,
                false
        );

        // 1. Đẩy câu trả lời mới
        answersRef.child(newAnswerId).setValue(newAnswer)
                .addOnSuccessListener(unused -> {
                    // 2. Xóa nội dung EditText
                    etComment.setText("");

                    // 3. Thêm vào RecyclerView (hiển thị ngay)
                    answerList.add(newAnswer);
                    answerAdapter.notifyItemInserted(answerList.size() - 1);
                    rvAnswers.scrollToPosition(answerList.size() - 1);

                    // 4. Tăng comment trong bảng questions
                    questionsRef.child("comment").runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            Integer current = currentData.getValue(Integer.class);
                            if (current == null) current = 0;
                            currentData.setValue(current + 1);
                            return Transaction.success(currentData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                            if (committed) {
                                Toast.makeText(PostDetailActivity.this, "Đã gửi câu trả lời", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(PostDetailActivity.this, "Gửi thành công nhưng không thể tăng comment", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi gửi câu trả lời", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateReputationScore(String userId, OnReputationUpdatedListener listener) {
        DatabaseReference votesRef = FirebaseDatabase.getInstance().getReference("votes");
        DatabaseReference questionsRef = FirebaseDatabase.getInstance().getReference("questions");
        DatabaseReference answersRef = FirebaseDatabase.getInstance().getReference("answers");

        questionsRef.orderByChild("authorUserId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot questionSnapshot) {
                Set<String> userQuestionIds = new HashSet<>();
                for (DataSnapshot ds : questionSnapshot.getChildren()) {
                    userQuestionIds.add(ds.getKey());
                }
                answersRef.orderByChild("authorUserId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot answerSnapshot) {
                        Set<String> userAnswerIds = new HashSet<>();
                        for (DataSnapshot ds : answerSnapshot.getChildren()) {
                            userAnswerIds.add(ds.getKey());
                        }
                        votesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot voteSnapshot) {
                                int upVote = 0;
                                int downVote = 0;
                                for (DataSnapshot voteDs : voteSnapshot.getChildren()) {
                                    Map<String, Object> vote = (Map<String, Object>) voteDs.getValue();
                                    if (vote == null) continue;
                                    Long voteType = (Long) vote.get("voteType");
                                    String qId = (String) vote.get("questionId");
                                    String aId = (String) vote.get("answerId");

                                    if ((qId != null && userQuestionIds.contains(qId)) ||
                                            (aId != null && userAnswerIds.contains(aId))) {
                                        if (voteType == 1) upVote++;
                                        else if (voteType == -1) downVote++;
                                    }
                                }
                                int reputation = upVote - downVote;
                                FirebaseDatabase.getInstance().getReference("users")
                                        .child(userId).child("reputationScore")
                                        .setValue(reputation)
                                        .addOnSuccessListener(unused -> {
                                            if (listener != null) {
                                                listener.onReputationUpdated(reputation);
                                            }
                                        });
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    public interface OnReputationUpdatedListener {
        void onReputationUpdated(int reputation);
    }
}
