package com.malikyasir.landlawassist.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.malikyasir.landlawassist.Modelss.Case;
import com.malikyasir.landlawassist.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CaseAdapter extends RecyclerView.Adapter<CaseAdapter.CaseViewHolder> {
    private List<Case> cases;
    private final OnCaseClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final FirebaseFirestore db;

    public interface OnCaseClickListener {
        void onCaseClick(Case caseItem);
        void onCaseDelete(Case caseItem, int position);
    }

    public CaseAdapter(List<Case> cases, OnCaseClickListener listener) {
        this.cases = cases;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public CaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_case, parent, false);
        return new CaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CaseViewHolder holder, int position) {
        Case caseItem = cases.get(position);
        holder.titleText.setText(caseItem.getTitle() != null ? caseItem.getTitle() : "");
        holder.caseNumberText.setText(caseItem.getCaseNumber() != null ? caseItem.getCaseNumber() : "");
        holder.courtText.setText(caseItem.getCourt() != null ? caseItem.getCourt() : "");
        
        if (caseItem.getFilingDate() > 0) {
            holder.filingDateText.setText(dateFormat.format(new Date(caseItem.getFilingDate())));
        } else {
            holder.filingDateText.setText("");
        }
        
        // Set status chip text and color
        String status = caseItem.getStatus();
        if (status != null) {
            holder.statusChip.setText(status);
            
            // Set chip color based on status
            int chipColor;
            switch (status) {
                case "ACTIVE":
                    chipColor = holder.itemView.getContext().getResources().getColor(R.color.status_active);
                    break;
                case "PENDING":
                    chipColor = holder.itemView.getContext().getResources().getColor(R.color.status_pending);
                    break;
                case "CLOSED":
                    chipColor = holder.itemView.getContext().getResources().getColor(R.color.status_closed);
                    break;
                default:
                    chipColor = holder.itemView.getContext().getResources().getColor(R.color.colorPrimary);
                    break;
            }
            // For TextView, we need to set the background color directly
            holder.statusChip.getBackground().setColorFilter(chipColor, android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            holder.statusChip.setText("UNKNOWN");
        }
        
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (caseItem.getUserId().equals(currentUserId)) {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCaseDelete(caseItem, position);
                }
            });
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }
        
        holder.viewDetailsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCaseClick(caseItem);
            }
        });
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCaseClick(caseItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cases.size();
    }

    public void updateCases(List<Case> newCases) {
        this.cases = newCases;
        notifyDataSetChanged();
    }

    static class CaseViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, caseNumberText, courtText, filingDateText;
        MaterialButton deleteButton, viewDetailsButton;
        TextView statusChip;

        CaseViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.caseTitleText);
            caseNumberText = itemView.findViewById(R.id.caseNumberText);
            courtText = itemView.findViewById(R.id.courtText);
            filingDateText = itemView.findViewById(R.id.filingDateText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
            statusChip = itemView.findViewById(R.id.statusChip);
        }
    }
} 