package com.lazamelezi.soundrecorder.adapters;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lazamelezi.soundrecorder.R;
import com.lazamelezi.soundrecorder.Helper.RecordingItem;
import com.lazamelezi.soundrecorder.background.DeleteAll;
import com.lazamelezi.soundrecorder.database.DBHelper;
import com.lazamelezi.soundrecorder.databinding.CardViewBinding;
import com.lazamelezi.soundrecorder.fragments.FileViewerFragment;
import com.lazamelezi.soundrecorder.fragments.PlaybackFragment;
import com.lazamelezi.soundrecorder.listeners.OnDatabaseChangedListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.lazamelezi.soundrecorder.service.RecordingService.getRealPathFromURI;
import static com.lazamelezi.soundrecorder.fragments.FileViewerFragment.SOUND_RECORDER_WITH_SEP;

public class FileViewerAdapter extends RecyclerView.Adapter<FileViewerAdapter.RecordingsViewHolder> implements OnDatabaseChangedListener {

    public static final String DIALOG_PLAYBACK = "dialog_playback";
    private static final String LOG_TAG = "FileViewerAdapterLog";
    private final SparseBooleanArray selectedItems;
    private final DBHelper mDatabase;
    AppCompatActivity appCompatActivity;
    LinearLayoutManager linearLayoutManager;
    FileViewerFragment fileViewerFragment;
    private RecordingItem recordingItem;
    private int selectedIndex = -1;
    private boolean isSelectModeOn = false;

    public FileViewerAdapter(AppCompatActivity appCompatActivity, FileViewerFragment fileViewerFragment, LinearLayoutManager linearLayoutManager) {
        super();

        this.appCompatActivity = appCompatActivity;
        this.fileViewerFragment = fileViewerFragment;
        this.linearLayoutManager = linearLayoutManager;

        selectedItems = new SparseBooleanArray();
        mDatabase = new DBHelper(appCompatActivity);
        DBHelper.setOnDatabaseChangedLister(this);

    }

    public boolean isSelectModeOn() {
        return isSelectModeOn;
    }

    public void setSelectModeOn(boolean selectModeOn) {
        isSelectModeOn = selectModeOn;
    }

    @NonNull
    @Override
    public RecordingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        CardViewBinding cardViewBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.card_view, parent, false);

        return new RecordingsViewHolder(cardViewBinding);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull RecordingsViewHolder holder, int position) {

        recordingItem = getItem(position);

        Log.d("DateLog", "Milisec - " + recordingItem.getTime());

        long itemDuration = recordingItem.getLength();

        long minutes = TimeUnit.MILLISECONDS.toMinutes(itemDuration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(itemDuration) - TimeUnit.MINUTES.toSeconds(minutes);

        holder.mCardViewBinding.fileNameText.setText(recordingItem.getName());
        holder.mCardViewBinding.fileLengthText.setText(String.format("%02d:%02d", minutes, seconds));

        holder.mCardViewBinding.fileDateAddedText.setText(
                DateUtils.formatDateTime(appCompatActivity,
                        recordingItem.getTime(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR
                ));

        holder.mCardViewBinding.cardView.setActivated(selectedItems.get(position, false));

        holder.mCardViewBinding.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isSelectModeOn) {

                    toggleSelection(position);

                } else {
                    PlaybackFragment playbackFragment = new PlaybackFragment().newInstance(getItem(holder.getLayoutPosition()), appCompatActivity);
                    FragmentTransaction transaction = appCompatActivity.getSupportFragmentManager().beginTransaction();
                    playbackFragment.show(transaction, DIALOG_PLAYBACK);
                }


            }
        });

        holder.mCardViewBinding.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {


                ArrayList<String> entries = new ArrayList<>();
                entries.add(appCompatActivity.getString(R.string.share_file));
                entries.add(appCompatActivity.getString(R.string.rename_file));
                entries.add(appCompatActivity.getString(R.string.delete_file));

                final CharSequence[] items = entries.toArray(new CharSequence[entries.size()]);

                AlertDialog.Builder builder = new AlertDialog.Builder(appCompatActivity);
                builder.setTitle(R.string.options);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                shareFileDialog(holder.getLayoutPosition());
                                break;
                            case 1:
                                renameFileDialog(holder.getLayoutPosition());
                                break;
                            case 2:
                                deleteFileDialog(holder.getLayoutPosition());
                                break;
                            default:
                                break;
                        }

                    }
                });
                builder.setCancelable(true);

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();

                return false;
            }
        });

        toggleIcon(holder.mCardViewBinding, position);
    }

    private void toggleIcon(CardViewBinding mCardViewBinding, int position) {
        if (selectedItems.get(position, false)) {
            mCardViewBinding.imageView.setImageResource(R.drawable.ic_selected);
        } else {
            mCardViewBinding.imageView.setImageResource(R.drawable.ic_mic);
        }

        if (selectedIndex == position)
            selectedIndex = -1;

    }

    private void deleteFileDialog(int position) {

        AlertDialog.Builder renameFileBuilder = new AlertDialog.Builder(appCompatActivity);
        renameFileBuilder.setTitle(R.string.confirm_delete);
        renameFileBuilder.setMessage(R.string.delete_message);
        renameFileBuilder.setCancelable(true);
        renameFileBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                remove(position);

                dialog.cancel();
            }
        });

        renameFileBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = renameFileBuilder.create();
        alertDialog.show();

    }

    private void remove(int position) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            final String where = MediaStore.MediaColumns.DISPLAY_NAME + "=?";

            final String[] selectionArgs = new String[]{
                    getItem(position).getName()
            };

            final ContentResolver contentResolver = appCompatActivity.getContentResolver();
            final Uri mFileURI = MediaStore.Files.getContentUri("external");

            contentResolver.delete(mFileURI, where, selectionArgs);

        } else {

            File mFileDelete = new File(getItem(position).getFilePath());
            if (mFileDelete.delete()) {
                Log.d(LOG_TAG, "File Deleted :" + mFileDelete.getAbsolutePath());
            } else {
                Log.d(LOG_TAG, "File Not Deleted :" + mFileDelete.getAbsolutePath());
            }

        }

        mDatabase.removeItemWithId(getItem(position).getId());
        notifyItemRemoved(position);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                fileViewerFragment.checkData();

            }
        }, 300);

    }

    private void renameFileDialog(int position) {

        AlertDialog.Builder renameFileBuilder = new AlertDialog.Builder(appCompatActivity);
        LayoutInflater inflater = LayoutInflater.from(appCompatActivity);

        View view = inflater.inflate(R.layout.dialog_rename_file, null);

        final EditText input = view.findViewById(R.id.new_name);

        renameFileBuilder.setTitle(appCompatActivity.getString(R.string.rename_file));
        renameFileBuilder.setCancelable(true);
        renameFileBuilder.setPositiveButton(appCompatActivity.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (input.getText().toString().trim().isEmpty())
                    return;

                String value = input.getText().toString().trim() + appCompatActivity.getString(R.string.mp3);
                rename(position, value);

                dialog.cancel();
            }
        });

        renameFileBuilder.setNegativeButton(appCompatActivity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        renameFileBuilder.setView(view);
        AlertDialog alertDialog = renameFileBuilder.create();
        alertDialog.show();

    }

    public boolean removeOutOfApp() {
        if (mDatabase.getCount() > 0) {
            DeleteAll deleteAll;
            deleteAll = new DeleteAll(appCompatActivity, this, mDatabase);
            deleteAll.execute();
            return true;
        } else {
            return false;
        }
    }

    private void rename(int position, String name) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContentResolver mContentResolver = appCompatActivity.getContentResolver();
            ContentValues mContentValues = new ContentValues();
            mContentValues.put(MediaStore.Audio.Media.DISPLAY_NAME, name);

            Uri uri = getRealPathFromURI(appCompatActivity, getItem(position).getName());
            mContentResolver.update(uri, mContentValues, null, null);
            mDatabase.renameItem(getItem(position), name, String.valueOf(uri));
            notifyItemChanged(position);

        } else {
            String mFilePath;
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath += SOUND_RECORDER_WITH_SEP + "/" + name;

            Log.d(LOG_TAG, "mFilePath : " + mFilePath);

            File newFilePath = new File(mFilePath);

            if (newFilePath.exists() && !newFilePath.isDirectory()) {
                Toast.makeText(appCompatActivity, String.format(appCompatActivity.getString(R.string.toast_file_exists), name), Toast.LENGTH_SHORT).show();
            } else {
                File oldFilePath = new File(getItem(position).getFilePath());
                oldFilePath.renameTo(newFilePath);
                mDatabase.renameItem(getItem(position), name, mFilePath);
                notifyItemChanged(position);
            }

        }


    }

    private void shareFileDialog(int position) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, shareFile(position));
        shareIntent.setType("audio/mp4");
        appCompatActivity.startActivity(Intent.createChooser(shareIntent, appCompatActivity.getString(R.string.send_to)));

    }

    private Uri shareFile(int position) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getRealPathFromURI(appCompatActivity, getItem(position).getName());
        } else {
            return Uri.fromFile(new File(getItem(position).getFilePath()));
        }

    }

    private RecordingItem getItem(int position) {
        return mDatabase.getItemAt(position);
    }

    @Override
    public int getItemCount() {
        return mDatabase.getCount();
    }

    @Override
    public void onNewDatabaseEntryAdded() {
        notifyItemInserted(getItemCount() - 1);
        linearLayoutManager.scrollToPosition(getItemCount() - 1);
    }

    @Override
    public void onDatabaseEntryRenamed() {

    }

    public void checkIfFileIsDeletedOutSide(List<String> deletedFiles) {

        if (deletedFiles.size() == 0) {

            if (mDatabase.getCount() > 0) {
                mDatabase.deleteAllData();
                notifyDataSetChanged();
                Log.d(LOG_TAG, "No File Exist Delete Database");
            } else {
                Log.d(LOG_TAG, "Everything is Clear");
            }

        } else {

            List<Integer> needToBeDeleted = new ArrayList<>();

            for (int i = 0; i < mDatabase.getCount(); i++) {

                if (mDatabase.getItemAt(i) != null) {

                    if (!deletedFiles.contains(mDatabase.getItemAt(i).getName())) {
                        Log.d(LOG_TAG, "File Don't Exist More - " + mDatabase.getItemAt(i).getName());
                        needToBeDeleted.add(mDatabase.getItemAt(i).getId());

                    }
                }
            }

            for (int id : needToBeDeleted) {
                mDatabase.removeItemWithId(id);
            }


        }

    }

    private void toggleSelection(int position) {

        selectedIndex = position;
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        } else {
            selectedItems.put(position, true);
        }

        notifyItemChanged(position);

        int count = selectedItemCount();
        fileViewerFragment.actionMode.setTitle(appCompatActivity.getString(R.string.selected, count));
        fileViewerFragment.actionMode.invalidate();


    }

    private int selectedItemCount() {
        return selectedItems.size();
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public void removeItems(int position) {
        remove(position);
        selectedIndex = -1;
    }

    public static class RecordingsViewHolder extends RecyclerView.ViewHolder {

        CardViewBinding mCardViewBinding;

        public RecordingsViewHolder(@NonNull CardViewBinding cardViewBinding) {
            super(cardViewBinding.getRoot());

            mCardViewBinding = cardViewBinding;

        }
    }
}
