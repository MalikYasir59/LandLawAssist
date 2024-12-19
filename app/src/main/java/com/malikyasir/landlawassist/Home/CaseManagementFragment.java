package com.malikyasir.landlawassist.Home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.malikyasir.landlawassist.R;
import com.malikyasir.landlawassist.usersidework.addcases; // Import your addcases DialogFragment

public class CaseManagementFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_case_management, container, false);

        // Initialize the Floating Action Button (FAB)
        FloatingActionButton addCaseFab = view.findViewById(R.id.addCaseFab);

        // Set click listener for the FAB
        addCaseFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create an instance of the addcases fragment (DialogFragment)
                addcases fragment = new addcases();
                // Show the fragment using DialogFragment
                fragment.show(getChildFragmentManager(), fragment.getTag());
            }
        });

        return view;
    }
}
