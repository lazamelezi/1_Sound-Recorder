package com.lazamelezi.soundrecorder.listeners;

public interface OnDatabaseChangedListener {
    void onNewDatabaseEntryAdded();

    void onDatabaseEntryRenamed();
}
