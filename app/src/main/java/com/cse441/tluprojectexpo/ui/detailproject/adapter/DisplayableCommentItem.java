package com.cse441.tluprojectexpo.ui.detailproject.adapter;

import com.cse441.tluprojectexpo.model.Comment;

// Có thể đặt trong package adapter hoặc model
public class DisplayableCommentItem {
    public static final int TYPE_ROOT_COMMENT = 0;
    public static final int TYPE_REPLY_COMMENT = 1;

    public Comment comment;
    public int type;
    public int depth; // Độ sâu của reply, dùng để tính padding

    public DisplayableCommentItem(Comment comment, int type, int depth) {
        this.comment = comment;
        this.type = type;
        this.depth = depth;
    }
}
