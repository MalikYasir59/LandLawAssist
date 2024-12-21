package com.malikyasir.landlawassist.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
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

    public interface OnCaseClickListener {
        void onCaseClick(Case caseItem);
    }

    public CaseAdapter(List<Case> cases, OnCaseClickListener listener) {
        this.cases = cases;
        this.listener = listener;
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
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && caseItem.getId() != null) {
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

        CaseViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.caseTitleText);
            caseNumberText = itemView.findViewById(R.id.caseNumberText);
            courtText = itemView.findViewById(R.id.courtText);
            filingDateText = itemView.findViewById(R.id.filingDateText);
        }
    }
} 