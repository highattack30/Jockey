package com.marverenic.music.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.marverenic.music.R;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Prefs;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.utils.Util;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

public class PreferenceFragment extends PreferenceFragmentCompat implements View.OnLongClickListener {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            int padding = (int) getResources().getDimension(R.dimen.global_padding);
            getListView().setPadding(padding, 0, padding, 0);

            getListView().setBackgroundColor(Themes.getBackground());
            setDividerHeight(0); // use custom divider decoration

            getListView().addItemDecoration(new BackgroundDecoration(
                    Themes.getBackgroundElevated(), android.R.id.title));
            getListView().addItemDecoration(
                    new DividerDecoration(getContext(), R.id.subheaderFrame));
        }
        return view;
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        return new PreferenceGroupAdapter(preferenceScreen) {
            @Override
            public void onBindViewHolder(PreferenceViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);

                // Override Equalizer preference attachment to add a long click listener
                // and to change the detail text at runtime
                if ("com.marverenic.music.fragments.EqualizerFragment"
                        .equals(getItem(position).getFragment())) {

                    ViewGroup itemView = (ViewGroup) holder.itemView;
                    TextView title = (TextView) itemView.findViewById(android.R.id.title);
                    TextView detail = (TextView) itemView.findViewById(android.R.id.summary);

                    if (Util.getSystemEqIntent(getContext()) != null && Util.hasEqualizer()) {
                        // If we have Jockey's Equalizer and another Equalizer
                        itemView.setOnLongClickListener(PreferenceFragment.this);
                        detail.setText(R.string.equalizer_more_options_detail);
                        detail.setVisibility(View.VISIBLE);
                    } else if (Util.getSystemEqIntent(getContext()) == null && !Util.hasEqualizer()) {
                        // If we don't have any equalizers
                        detail.setText(R.string.equalizerUnsupported);
                        detail.setVisibility(View.VISIBLE);
                        itemView.setEnabled(false);
                        title.setEnabled(false);
                        detail.setEnabled(false);
                    }
                }
            }
        };
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof ListPreference) {
            final ListPreference listPref = (ListPreference) preference;

            final AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setSingleChoiceItems(
                            listPref.getEntries(),
                            listPref.findIndexOfValue(listPref.getValue()),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    listPref.setValueIndex(which);
                                    dialog.dismiss();
                                }
                            }
                    )
                    .setTitle(preference.getTitle())
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.header_settings);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (EqualizerFragment.class.getName().equals(preference.getFragment())) {
            Intent eqIntent = Util.getSystemEqIntent(getActivity());

            if (eqIntent != null
                    && !Prefs.getPrefs(getActivity()).getBoolean(Prefs.EQ_ENABLED, false)) {
                // If the system has an equalizer implementation already in place, use it
                // to avoid weird problems and conflicts that can cause unexpected behavior

                // for example, on Motorola devices, attaching an Equalizer can cause the
                // MediaPlayer's volume to briefly become very loud -- even when the phone
                // is muted
                startActivity(eqIntent);
            } else if (Util.hasEqualizer()) {
                // If there isn't a global equalizer or the user has already enabled our
                // equalizer, navigate to the built-in implementation
                Navigate.to(getActivity(), new EqualizerFragment(), R.id.prefFrame);
            } else {
                Toast.makeText(getActivity(), R.string.equalizerUnsupported, Toast.LENGTH_LONG)
                        .show();
            }
            return true;
        } else if (DirectoryListFragment.class.getName().equals(preference.getFragment())) {
            Fragment fragment = new DirectoryListFragment();
            Bundle args = new Bundle();
            args.putString(DirectoryListFragment.PREFERENCE_EXTRA, preference.getKey());
            args.putString(DirectoryListFragment.TITLE_EXTRA, preference.getTitle().toString());
            fragment.setArguments(args);

            Navigate.to(getActivity(), fragment, R.id.prefFrame);
            return true;
        } else if (Prefs.ADD_SHORTCUT.equals(preference.getKey())) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.add_shortcut)
                    .setMessage(R.string.add_shortcut_description)
                    .setPositiveButton(R.string.action_add,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Themes.updateLauncherIcon(getActivity());
                                }
                            })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onLongClick(View v) {
        if (Util.hasEqualizer()) {
            Navigate.to(getActivity(), new EqualizerFragment(), R.id.prefFrame);
        } else {
            Toast
                    .makeText(
                            getActivity(),
                            R.string.equalizerUnsupported,
                            Toast.LENGTH_LONG)
                    .show();
        }
        return true;
    }
}