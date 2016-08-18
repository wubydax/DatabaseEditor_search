package com.wubydax.dbeditor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/*      Created by Roberto Mariani and Anna Berkovitch, 26/03/16
        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

public class TableValuesFragment extends Fragment implements CheckSu.OnExecutedListener {
    public static final String TABLE_NAME = "table_name";
    private RecyclerView mRecyclerView;
    private String mTableName;
    private List<TableItems> mList;
    private EditText mSearch;
    private ContentResolver mContentResolver;

    public TableValuesFragment() {
    }

    public static TableValuesFragment newInstance(String tableName) {
        Bundle extras = new Bundle();
        extras.putString(TABLE_NAME, tableName);
        TableValuesFragment tableValuesFragment = new TableValuesFragment();
        tableValuesFragment.setArguments(extras);
        return tableValuesFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mContentResolver = getActivity().getContentResolver();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        showNewEntryDialog();
        return super.onOptionsItemSelected(item);
    }

    private void showNewEntryDialog() {
        @SuppressLint("InflateParams") final View view = LayoutInflater.from(getActivity()).inflate(R.layout.new_entry_dialog_layout, null);
        final EditText newKey = (EditText) view.findViewById(R.id.new_entry_key);
        final EditText newValue = (EditText) view.findViewById(R.id.new_entry_value);
        new AlertDialog.Builder(getActivity())
                .setTitle("Add entry to current table")
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String key = newKey.getText().toString();
                        String value = newValue.getText().toString();
                        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                            switch (mTableName) {
                                case "system":
                                    updateSystem(key, value);
                                    break;
                                case "global":
                                    updateGlobal(key, value);
                                    break;
                                case "secure":
                                    updateSecure(key, value);
                                    break;
                            }
                        } else {
                            Toast.makeText(getActivity(), "Key or value cannot be empty", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).create()
                .show();
    }

    private void updateSecure(String key, String value) {
        if (TextUtils.isEmpty(Settings.Secure.getString(mContentResolver, key))) {
            Settings.Secure.putString(mContentResolver, key, value);
            updateAdapter(key, value);
        } else {
            Toast.makeText(getActivity(), "Entry already exists", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateGlobal(String key, String value) {
        if (TextUtils.isEmpty(Settings.Global.getString(mContentResolver, key))) {
            Settings.Global.putString(mContentResolver, key, value);
            updateAdapter(key, value);
        } else {
            Toast.makeText(getActivity(), "Entry already exists", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSystem(String key, String value) {
        if (TextUtils.isEmpty(Settings.System.getString(mContentResolver, key))) {
            Settings.System.putString(mContentResolver, key, value);
            updateAdapter(key, value);
        } else {
            Toast.makeText(getActivity(), "Entry already exists", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAdapter(String key, String value) {
        TableItems tableItems = new TableItems();
        tableItems.key = key;
        tableItems.value = value;
        mList.add(tableItems);
        if (mRecyclerView.getAdapter() != null) {
            ((TableValuesAdapter) mRecyclerView.getAdapter()).updateList(tableItems);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mList = new ArrayList<>();
        mTableName = getArguments().getString(TABLE_NAME);
        View rootView = inflater.inflate(R.layout.fragment_table_values, container, false);
        mSearch = (EditText) rootView.findViewById(R.id.searchKey);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        CheckSu checkIt = new CheckSu(getActivity(), mTableName);
        checkIt.setOnExecutedListener(this);
        checkIt.execute();
        return rootView;
    }

    @Override
    public void onExecuted() {
        String key, value;
        BufferedReader bufferedReader;
        try {
            File file = new File("/system/etc/current_db_" + mTableName + ".xml");
            bufferedReader = new BufferedReader(new FileReader(file));

            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                try {

                    if (line.contains("id=")) {
                        if (line.contains("value=")) {
                            key = line.substring(line.indexOf("name=") + 6, line.indexOf("value=") - 2);
                            value = line.substring(line.indexOf("value=") + 7, line.indexOf("package=") - 2);
                        } else {
                            key = line.substring(line.indexOf("name=") + 6, line.indexOf("package=") - 2);
                            value = "";
                        }
                        TableItems tableItems = new TableItems();
                        tableItems.key = key;
                        tableItems.value = value;
                        mList.add(tableItems);

                    }
                } catch (Exception e) {
                    //Catch exception if any
                    System.err.println("Error: " + e.getMessage());
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        final TableValuesAdapter tableValuesAdapter = new TableValuesAdapter();
        mRecyclerView.setAdapter(tableValuesAdapter);
        mSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tableValuesAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


    }

    private void showUpdateDialog(final String key, final String value, final int position) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_layout, null, false);
        final TextView valueText = (TextView) view.findViewById(R.id.textValue);
        TextView keyText = (TextView) view.findViewById(R.id.textKey);
        final EditText editText = (EditText) view.findViewById(R.id.valueEditText);
        valueText.setText(value);
        editText.setText(value);
        keyText.setText(key);
        new AlertDialog.Builder(getActivity())
                .setTitle("Change value")
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newValue = editText.getText().toString();
                        boolean isGranted = Settings.System.canWrite(getActivity());
                        if (!isGranted) {
                            Intent grantPermission = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                            startActivity(grantPermission);
                        } else {
                            switch (mTableName) {
                                case "system":
                                    Settings.System.putString(mContentResolver, key, newValue);
                                    break;
                                case "global":
                                    Settings.Global.putString(mContentResolver, key, newValue);
                                    break;
                                case "secure":
                                    Settings.Secure.putString(mContentResolver, key, newValue);
                                    break;
                            }
                            mList.get(position).value = newValue;

                        }

                        ((TableValuesAdapter) mRecyclerView.getAdapter()).notifyDataSetChanged();
                    }
                }).show();

    }



    public class TableValuesAdapter extends RecyclerView.Adapter<TableValuesAdapter.ViewHolder> implements Filterable {
        private List<TableItems> mOriginalList;

        TableValuesAdapter() {
            mOriginalList = mList;
        }

        @Override
        public TableValuesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row, parent, false);

            return new ViewHolder(v);
        }


        @Override
        public void onBindViewHolder(TableValuesAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
            holder.keyTextView.setText(mList.get(position).key);
            holder.valueTextView.setText(mList.get(position).value);
            holder.mPosition = position;


        }

        @Override
        public int getItemCount() {
            return mList != null ? mList.size() : 0;
        }

        public Filter getFilter() {
            final List<TableItems> list = new ArrayList<>();

            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    for (TableItems tableItems : mOriginalList) {
                        if (tableItems.key.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            list.add(tableItems);
                        }
                    }
                    FilterResults filterResults = new FilterResults();
                    filterResults.count = list.size();
                    filterResults.values = list;
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    mList = (List<TableItems>) results.values;
                    notifyDataSetChanged();
                }
            };
        }

        void updateList(TableItems tableItems) {
            mOriginalList.add(tableItems);
            notifyDataSetChanged();
        }

        public TableItems getItem(int position) {
            return mList != null ? mList.get(position) : null;
        }

        private void showDeleteDialog(final TableItems tableItems) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Delete entry?")
                    .setMessage("Are you sure you want to delete " + tableItems.key + " from " + mTableName + " table?")
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Command command = new Command(0, "settings delete " + mTableName + " " + tableItems.key) {
                                @Override
                                public void commandCompleted(int id, int exitcode) {
                                    super.commandCompleted(id, exitcode);
                                    if (exitcode != 0) {
                                        Toast.makeText(getActivity(), "Failed to delete value", Toast.LENGTH_SHORT).show();
                                    } else {
                                        mList.remove(tableItems);
                                        mOriginalList.remove(tableItems);
                                        notifyDataSetChanged();
                                    }
                                }
                            };
                            try {
                                RootTools.getShell(true).add(command);
                            } catch (IOException | TimeoutException e) {
                                e.printStackTrace();
                            } catch (RootDeniedException e) {
                                Toast.makeText(getActivity(), "Failed to acquire root privileges", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).create()
                    .show();
        }


        class ViewHolder extends RecyclerView.ViewHolder {
            TextView keyTextView;
            TextView valueTextView;
            int mPosition;

            ViewHolder(View itemView) {
                super(itemView);
                keyTextView = (TextView) itemView.findViewById(R.id.keyTextView);
                valueTextView = (TextView) itemView.findViewById(R.id.valueTextView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String key = mList.get(mPosition).key;
                        final String value = mList.get(mPosition).value;
                        showUpdateDialog(key, value, mPosition);

                    }
                });
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        showDeleteDialog(getItem(mPosition));
                        return true;
                    }
                });
            }
        }
    }

    public class TableItems {
        String key;
        String value;
    }

}
