package com.cse441.tluprojectexpo.adapter;

// Trong package adapter của bạn
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R; // Thay thế bằng package của bạn
import com.cse441.tluprojectexpo.model.Member; // Thay thế bằng package của bạn
import java.util.ArrayList;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class MemberSearchAdapter extends RecyclerView.Adapter<MemberSearchAdapter.MemberViewHolder> {

    private Context context;
    private List<Member> memberListFull; // Danh sách đầy đủ ban đầu
    private List<Member> memberListFiltered; // Danh sách đã được lọc để hiển thị

    // Interface để xử lý click (nếu bạn muốn chọn thành viên)
    public interface OnMemberClickListener {
        void onMemberClick(Member member);
    }
    private OnMemberClickListener listener;

    public MemberSearchAdapter(Context context, List<Member> memberList, OnMemberClickListener listener) {
        this.context = context;
        this.memberListFull = new ArrayList<>(memberList); // Tạo bản sao để không ảnh hưởng list gốc
        this.memberListFiltered = new ArrayList<>(memberList);
        this.listener = listener;
    }

    public void updateData(List<Member> newMembers) {
        this.memberListFull.clear();
        this.memberListFull.addAll(newMembers);
        filter(""); // Hiển thị tất cả khi cập nhật
    }


    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member_search, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = memberListFiltered.get(position);
        holder.tvMemberName.setText(member.getName());
        holder.tvMemberClass.setText(member.getClassName());

        Glide.with(context)
                .load(member.getAvatarUrl())
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar) // Ảnh hiển thị nếu có lỗi tải
                .into(holder.ivMemberAvatar);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMemberClick(member);
            }
        });
    }

    @Override
    public int getItemCount() {
        return memberListFiltered.size();
    }

    public void filter(String query) {
        memberListFiltered.clear();
        if (TextUtils.isEmpty(query)) {
            memberListFiltered.addAll(memberListFull);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (Member member : memberListFull) {
                // Tìm kiếm theo tên hoặc lớp
                if ((member.getName() != null && member.getName().toLowerCase().contains(filterPattern)) ||
                        (member.getClassName() != null && member.getClassName().toLowerCase().contains(filterPattern))) {
                    memberListFiltered.add(member);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivMemberAvatar;
        TextView tvMemberName;
        TextView tvMemberClass;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMemberAvatar = itemView.findViewById(R.id.iv_member_avatar_search);
            tvMemberName = itemView.findViewById(R.id.tv_member_name_search);
            tvMemberClass = itemView.findViewById(R.id.tv_member_class_search);
        }
    }
}
