package com.cse441.tluprojectexpo.admin;

import com.cse441.tluprojectexpo.model.Comment;

public class DisplayableCommentItem {
    public static final int TYPE_ROOT_COMMENT = 0;
    public static final int TYPE_REPLY_COMMENT = 1;

    public Comment comment;
    public int type;
    public int depth; // Độ sâu của bình luận, 0 là gốc

    public DisplayableCommentItem(Comment comment, int type, int depth) {
        this.comment = comment;
        this.type = type;
        this.depth = depth;
    }
}
