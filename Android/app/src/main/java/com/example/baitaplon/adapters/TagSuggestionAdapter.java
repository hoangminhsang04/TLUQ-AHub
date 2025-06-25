package com.example.baitaplon.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baitaplon.R;
import com.example.baitaplon.models.Tag;

import java.util.List;

public class TagSuggestionAdapter extends RecyclerView.Adapter<TagSuggestionAdapter.TagViewHolder> {
    private Context context;
    private List<Tag> tags;
    private OnItemClickListener listener;

    public TagSuggestionAdapter(Context context, List<Tag> tags) {
        this.context = context;
        this.tags = tags;
    }

    public void updateTags(List<Tag> newTags) {
        this.tags = newTags;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(Tag tag);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tag_suggestion, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        Tag tag = tags.get(position);
        holder.txtTag.setText("#" + tag.getTagName());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(tag);
        });
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    static class TagViewHolder extends RecyclerView.ViewHolder {
        TextView txtTag;

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTag = itemView.findViewById(R.id.txtTagName);
        }
    }
}
