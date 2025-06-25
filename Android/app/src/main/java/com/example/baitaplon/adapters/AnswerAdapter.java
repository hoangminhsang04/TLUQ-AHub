package com.example.baitaplon;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.baitaplon.R;
import com.example.baitaplon.TimeUtils;
import com.example.baitaplon.models.Answer;
import com.example.baitaplon.models.User;
import com.example.baitaplon.models.Vote;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnswerAdapter extends RecyclerView.Adapter<AnswerAdapter.AnswerViewHolder> {
    private Context context;
    private List<Answer> answerList;
    private DatabaseReference votesRef, answersRef;
    private SharedPreferences prefs;
    private OnVoteClearedListener voteClearedListener;
    private OnQuestionVoteCountChangedListener questionVoteCountChangedListener;
    private final Map<String, User> userCache = new HashMap<>();
    private final Map<String, ValueEventListener> userListeners = new HashMap<>();
    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
    private OnQuestionAuthorReputationChangedListener reputationChangedListener;

    public void setOnQuestionAuthorReputationChangedListener(OnQuestionAuthorReputationChangedListener listener) {
        this.reputationChangedListener = listener;
    }

    public AnswerAdapter(Context context, List<Answer> answerList, OnVoteClearedListener listener,OnQuestionVoteCountChangedListener questionVoteCountChangedListener) {
        this.context = context;
        this.answerList = answerList;
        this.voteClearedListener = listener;
        this.questionVoteCountChangedListener =questionVoteCountChangedListener;
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        votesRef = db.getReference("votes");
        answersRef = db.getReference("answers");
        prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
    }
    @NonNull
    @Override
    public AnswerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new AnswerViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull AnswerViewHolder holder, int position) {
        Answer answer = answerList.get(position);
        holder.tvCommentContent.setText(answer.getContent());
        holder.tvCommentTime.setText(TimeUtils.getRelativeTime(answer.getCreatedAt()));
        holder.tvCommentLikeCount.setText(String.valueOf(answer.getVoteCount()));
        if (answer.isAccepted()) {
            holder.btnStar.setColorFilter(Color.YELLOW);
        } else {
            holder.btnStar.setColorFilter(null);
        }

        String authorId = answer.getAuthorUserId();
        holder.itemView.setTag(authorId); // Đảm bảo gắn tag để kiểm tra holder đúng user

        // Nếu đã có user trong cache thì dùng luôn
        if (userCache.containsKey(authorId)) {
            bindUserToViewHolder(holder, userCache.get(authorId), authorId);
        } else {
            // Nếu chưa có listener thì mới gắn
            if (!userListeners.containsKey(authorId)) {
                ValueEventListener listener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            user.setUserId(snapshot.getKey());
                            userCache.put(authorId, user);
                            notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                };
                usersRef.child(authorId).addValueEventListener(listener);
                userListeners.put(authorId, listener);
            }
        }

        // Load vote status của người dùng
        String userId = prefs.getString("userId", null);
        if (userId != null) {
            votesRef.orderByChild("answerId").equalTo(answer.getAnswerId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot snap : snapshot.getChildren()) {
                                Vote vote = snap.getValue(Vote.class);
                                if (vote != null && userId.equals(vote.getUserId())) {
                                    updateVoteUI(holder, vote.getVoteType());
                                    break;
                                }
                            }
                        }

                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }

        // Xử lý Like
        holder.ivCommentLike.setOnClickListener(v -> handleVote(holder, answer, 1));
        // Xử lý Unlike
        holder.ivCommentUnlike.setOnClickListener(v -> handleVote(holder, answer, -1));

        // Đánh dấu câu trả lời hay nhất
        holder.btnStar.setOnClickListener(v -> {
            String currentUserId = prefs.getString("userId", null);
            if (currentUserId == null) {
                Toast.makeText(context, "Bạn cần đăng nhập để thực hiện thao tác này", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseDatabase.getInstance().getReference("questions")
                    .child(answer.getQuestionId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String authorUserId = snapshot.child("authorUserId").getValue(String.class);
                            String acceptedAnswerId = snapshot.child("acceptedAnswerId").getValue(String.class);
                            if (!currentUserId.equals(authorUserId)) {
                                Toast.makeText(context, "Chỉ người tạo câu hỏi mới có quyền chọn câu trả lời hay nhất", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (acceptedAnswerId != null && !acceptedAnswerId.isEmpty() && !acceptedAnswerId.equals(answer.getAnswerId())) {
                                Toast.makeText(context, "Bạn đã chọn câu trả lời hay nhất rồi!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            FirebaseDatabase.getInstance().getReference("answers")
                                    .child(answer.getAnswerId()).child("isAccepted").setValue(true);
                            FirebaseDatabase.getInstance().getReference("questions")
                                    .child(answer.getQuestionId()).child("acceptedAnswerId").setValue(answer.getAnswerId());
                            answer.setAccepted(true);
                            notifyDataSetChanged();
                            Toast.makeText(context, "Đã chọn là câu trả lời hay nhất", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("AnswerAdapter", "Lỗi truy vấn: " + error.getMessage());
                        }
                    });
        });
    }

    private void bindUserToViewHolder(AnswerViewHolder holder, User user, String authorId) {
        if (!authorId.equals(holder.itemView.getTag())) return;
        holder.tvCommentAuthor.setText(user.getFullName());
        holder.tvReputation.setText(user.getReputationScore() + " điểm");

        String avatarUrl = user.getAvatar();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.anhdaidien)
                    .error(R.drawable.anhdaidien)
                    .into(holder.ivCommentAuthorAvatar);
        } else {
            holder.ivCommentAuthorAvatar.setImageResource(R.drawable.anhdaidien);
        }
    }

    @Override
    public int getItemCount() {
        return answerList.size();
    }
    public static class AnswerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCommentAuthorAvatar, ivCommentLike, ivCommentUnlike;
        TextView tvCommentAuthor, tvCommentContent, tvCommentTime, tvCommentLikeCount;
        ImageButton btnStar;
        TextView tvReputation;
        public AnswerViewHolder(@NonNull View v) {
            super(v);
            ivCommentAuthorAvatar = v.findViewById(R.id.ivCommentAuthorAvatar);
            tvCommentAuthor = v.findViewById(R.id.tvCommentAuthor);
            tvCommentContent = v.findViewById(R.id.tvCommentContent);
            tvCommentTime = v.findViewById(R.id.tvCommentTime);
            tvCommentLikeCount = v.findViewById(R.id.tvCommentLikeCount);
            ivCommentLike = v.findViewById(R.id.ivCommentLike);
            ivCommentUnlike = v.findViewById(R.id.ivCommentUnlike);
            btnStar = v.findViewById(R.id.btnstar);
            tvReputation = v.findViewById(R.id.tvReputation);
        }
    }
    private void handleVote(AnswerViewHolder holder, Answer answer, int voteType) {
        String userId = prefs.getString("userId", null);
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = db.getReference("users");
        DatabaseReference answersRef = db.getReference("answers");

        if (userId == null) {
            Toast.makeText(context, "Bạn cần đăng nhập để vote", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference questionsRef = db.getReference("questions");
        DatabaseReference votesRef = db.getReference("votes");
        votesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingAnswerVoteId = null;
                String existingQuestionVoteId = null;
                int existingVoteType = 0;
                boolean hasVotedQuestion = false;
                boolean hasVotedAnswer = false;
                // Kiểm tra các vote hiện tại của user
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Vote vote = snap.getValue(Vote.class);
                    if (vote == null || !userId.equals(vote.getUserId())) continue;

                    if (vote.getAnswerId() != null && vote.getAnswerId().equals(answer.getAnswerId())) {
                        hasVotedAnswer = true;
                        existingAnswerVoteId = snap.getKey();
                        existingVoteType = vote.getVoteType();
                    } else if (vote.getQuestionId() != null && vote.getQuestionId().equals(answer.getQuestionId()) && vote.getAnswerId() == null) {
                        hasVotedQuestion = true;
                        existingQuestionVoteId = snap.getKey();
                    }
                }
                // Trường hợp đã vote cho câu trả lời
                if (hasVotedAnswer) {
                    if (existingVoteType == voteType) {
                        Toast.makeText(context, "Bạn đã vote câu trả lời này rồi", Toast.LENGTH_SHORT).show();
                    } else {
                        // Đổi loại vote, không thay đổi voteCount
                        votesRef.child(existingAnswerVoteId).child("voteType").setValue(voteType)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        updateVoteUI(holder, voteType);

                                        // ✅ Cập nhật lại điểm của người được vote
                                        String authorUserId = answer.getAuthorUserId();
                                        if (authorUserId != null) {
                                            updateReputationScore(authorUserId);
                                        }
                                    } else {
                                        Toast.makeText(context, "Lỗi khi cập nhật loại vote", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                    return;
                }
                // Nếu đã vote cho câu hỏi thì phải xóa trước
                if (hasVotedQuestion) {
                    votesRef.child(existingQuestionVoteId).removeValue();
                    // Sau khi xóa vote câu hỏi
                    questionsRef.child(answer.getQuestionId()).child("authorUserId")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    String questionAuthorId = dataSnapshot.getValue(String.class);
                                    if (questionAuthorId != null) {
                                        updateReputationScore(questionAuthorId);  // ✅ Trừ điểm user cũ
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) { }
                            });

                    if (voteClearedListener != null) {
                        voteClearedListener.onQuestionVoteCleared();
                    }
                    // Giảm voteCount của câu hỏi
                    questionsRef.child(answer.getQuestionId()).child("voteCount")
                            .runTransaction(new Transaction.Handler() {
                                @NonNull
                                @Override
                                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                    Integer count = currentData.getValue(Integer.class);
                                    if (count == null) count = 0;
                                    currentData.setValue(Math.max(0, count - 1));
                                    return Transaction.success(currentData);
                                }
                                @Override
                                public void onComplete(@androidx.annotation.Nullable DatabaseError error, boolean committed, @androidx.annotation.Nullable DataSnapshot snapshot) {
                                    if (committed && snapshot != null && questionVoteCountChangedListener != null) {
                                        Integer updatedCount = snapshot.getValue(Integer.class);
                                        questionVoteCountChangedListener.onQuestionVoteCountChanged(updatedCount);
                                    }
                                }
                            });
                }
                // Tạo mới vote cho câu trả lời
                String voteId = votesRef.push().getKey();
                Vote newVote = new Vote(voteId, userId, null, answer.getAnswerId(), voteType);
                votesRef.child(voteId).setValue(newVote);
                // Tăng voteCount cho câu trả lời
                answersRef.child(answer.getAnswerId()).child("voteCount")
                        .runTransaction(new Transaction.Handler() {
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                Integer count = currentData.getValue(Integer.class);
                                if (count == null) count = 0;
                                currentData.setValue(count + 1);
                                return Transaction.success(currentData);
                            }
                            @Override
                            public void onComplete(@androidx.annotation.Nullable DatabaseError error, boolean committed, @androidx.annotation.Nullable DataSnapshot snapshot) {
                                if (committed && snapshot != null) {
                                    int newVoteCount = snapshot.getValue(Integer.class);
                                    answer.setVoteCount(newVoteCount);
                                    holder.tvCommentLikeCount.setText(String.valueOf(newVoteCount));
                                    updateVoteUI(holder, voteType);
                                    notifyItemChanged(holder.getAdapterPosition());  // hoặc notifyDataSetChanged();
                                    if (answer.getAuthorUserId() != null) {
                                        updateReputationScore(answer.getAuthorUserId());
                                    }
                                }
                            }
                        });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Vote", "Lỗi khi vote: " + error.getMessage());
            }
        });
    }
    private void updateVoteUI(AnswerViewHolder holder, int voteType) {
        if (voteType == 1) {
            holder.ivCommentLike.setColorFilter(Color.BLUE);
            holder.ivCommentUnlike.setColorFilter(null);
        } else if (voteType == -1) {
            holder.ivCommentUnlike.setColorFilter(Color.RED);
            holder.ivCommentLike.setColorFilter(null);
        }
    }
    public void clearUserVotes(String userId) {
        for (int i = 0; i < answerList.size(); i++) {
            notifyItemChanged(i);
        }
    }
    public interface OnVoteClearedListener {
        void onQuestionVoteCleared(); // để PostDetailActivity reset màu vote câu hỏi
    }
    public interface OnQuestionVoteCountChangedListener {
        void onQuestionVoteCountChanged(int newCount);
    }
    public interface OnQuestionAuthorReputationChangedListener {
        void onQuestionAuthorReputationChanged(int newReputation);
    }
    public interface OnReputationChangedListener {
        void onReputationUpdated(int newReputation);
    }

    private void updateReputationScore(String userId) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        DatabaseReference votesRef = db.child("votes");
        DatabaseReference questionsRef = db.child("questions");
        DatabaseReference answersRef = db.child("answers");

        // Lấy tất cả câu hỏi của user
        questionsRef.orderByChild("authorUserId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot questionSnapshot) {
                Set<String> userQuestionIds = new HashSet<>();
                for (DataSnapshot ds : questionSnapshot.getChildren()) {
                    userQuestionIds.add(ds.getKey());
                }

                // Lấy tất cả câu trả lời của user
                answersRef.orderByChild("authorUserId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot answerSnapshot) {
                        Set<String> userAnswerIds = new HashSet<>();
                        for (DataSnapshot ds : answerSnapshot.getChildren()) {
                            userAnswerIds.add(ds.getKey());
                        }

                        // Lấy tất cả votes
                        votesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot voteSnapshot) {
                                int upVote = 0;
                                int downVote = 0;

                                for (DataSnapshot voteDs : voteSnapshot.getChildren()) {
                                    Vote vote = voteDs.getValue(Vote.class);
                                    if (vote == null) continue;

                                    String qId = vote.getQuestionId();
                                    String aId = vote.getAnswerId();

                                    if ((qId != null && userQuestionIds.contains(qId)) ||
                                            (aId != null && userAnswerIds.contains(aId))) {
                                        if (vote.getVoteType() == 1) upVote++;
                                        else if (vote.getVoteType() == -1) downVote++;
                                    }
                                }

                                int reputation = upVote - downVote;
                                db.child("users").child(userId).child("reputationScore")
                                        .setValue(reputation)
                                        .addOnSuccessListener(unused -> {
                                            if (reputationChangedListener != null) {
                                                reputationChangedListener.onQuestionAuthorReputationChanged(reputation);
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

}
