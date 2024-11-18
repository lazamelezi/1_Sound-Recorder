package com.lazamelezi.soundrecorder.listeners;

import java.util.List;

public interface UpdateListener {

    void update(String progress);

    void finish();

    void finish(List<String> deletedFiles);

}
