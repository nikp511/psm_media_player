package video.player.qrplayer.util;


import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import video.player.qrplayer.BuildConfig;
import video.player.qrplayer.activity.HomeActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class StateSaver {
    public static final String KEY_SAVED_STATE = "key_saved_state";
    private static final ConcurrentHashMap<String, Queue<Object>> stateObjectsHolder = new ConcurrentHashMap<>();
    private static final String TAG = "StateSaver";
    private static final String CACHE_DIR_NAME = "state_cache";
    private static String cacheDirPath;

    private StateSaver() {
        //no instance
    }

    public static void init(Context context) {
        File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir != null) cacheDirPath = externalCacheDir.getAbsolutePath();
        if (TextUtils.isEmpty(cacheDirPath)) cacheDirPath = context.getCacheDir().getAbsolutePath();
    }

    public static SavedState tryToRestore(Bundle outState, WriteRead writeRead) {
        if (outState == null || writeRead == null) return null;

        SavedState savedState = outState.getParcelable(KEY_SAVED_STATE);
        if (savedState == null) return null;

        return tryToRestore(savedState, writeRead);
    }

    @Nullable
    private static SavedState tryToRestore(@NonNull SavedState savedState, @NonNull WriteRead writeRead) {
        if (HomeActivity.DEBUG) {
            Log.d(TAG, "tryToRestore() called with: savedState = [" + savedState + "], writeRead = [" + writeRead + "]");
        }

        FileInputStream fileInputStream = null;
        try {
            Queue<Object> savedObjects = stateObjectsHolder.remove(savedState.getPrefixFileSaved());
            if (savedObjects != null) {
                writeRead.readFrom(savedObjects);
                if (HomeActivity.DEBUG) {
                    Log.d(TAG, "tryToSave: reading objects from holder > " + savedObjects + ", stateObjectsHolder > " + stateObjectsHolder);
                }
                return savedState;
            }

            File file = new File(savedState.getPathFileSaved());
            if (!file.exists()) {
                if (HomeActivity.DEBUG) {
                    Log.d(TAG, "Cache file doesn't exist: " + file.getAbsolutePath());
                }
                return null;
            }

            fileInputStream = new FileInputStream(file);
            ObjectInputStream inputStream = new ObjectInputStream(fileInputStream);
            //noinspection unchecked
            savedObjects = (Queue<Object>) inputStream.readObject();
            if (savedObjects != null) {
                writeRead.readFrom(savedObjects);
            }

            return savedState;
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore state", e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    @Nullable
    private static SavedState tryToSave(boolean isChangingConfig, final String prefixFileName, String suffixFileName, WriteRead writeRead) {
        if (HomeActivity.DEBUG) {
            Log.d(TAG, "tryToSave() called with: isChangingConfig = [" + isChangingConfig + "], prefixFileName = [" + prefixFileName + "], suffixFileName = [" + suffixFileName + "], writeRead = [" + writeRead + "]");
        }

        LinkedList<Object> savedObjects = new LinkedList<>();
        writeRead.writeTo(savedObjects);

        if (isChangingConfig) {
            if (savedObjects.size() > 0) {
                stateObjectsHolder.put(prefixFileName, savedObjects);
                return new SavedState(prefixFileName, "");
            } else {
                if (HomeActivity.DEBUG) Log.d(TAG, "Nothing to save");
                return null;
            }
        }

        FileOutputStream fileOutputStream = null;
        try {
            File cacheDir = new File(cacheDirPath);
            if (!cacheDir.exists())
                throw new RuntimeException("Cache dir does not exist > " + cacheDirPath);
            cacheDir = new File(cacheDir, CACHE_DIR_NAME);
            if (!cacheDir.exists()) {
                if (!cacheDir.mkdir()) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Failed to create cache directory " + cacheDir.getAbsolutePath());
                    }
                    return null;
                }
            }

            if (TextUtils.isEmpty(suffixFileName)) suffixFileName = ".cache";
            File file = new File(cacheDir, prefixFileName + suffixFileName);
            if (file.exists() && file.length() > 0) {
                // If the file already exists, just return it
                return new SavedState(prefixFileName, file.getAbsolutePath());
            } else {
                // Delete any file that contains the prefix
                File[] files = cacheDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.contains(prefixFileName);
                    }
                });
                for (File fileToDelete : files) {
                    fileToDelete.delete();
                }
            }

            fileOutputStream = new FileOutputStream(file);
            ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
            outputStream.writeObject(savedObjects);

            return new SavedState(prefixFileName, file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save state", e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    public interface WriteRead {

        String generateSuffix();

        void writeTo(Queue<Object> objectsToSave);

        void readFrom(@NonNull Queue<Object> savedObjects) throws Exception;
    }

    public static class SavedState implements Parcelable {
        @SuppressWarnings("unused")
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        private final String prefixFileSaved;
        private final String pathFileSaved;

        public SavedState(String prefixFileSaved, String pathFileSaved) {
            this.prefixFileSaved = prefixFileSaved;
            this.pathFileSaved = pathFileSaved;
        }

        protected SavedState(Parcel in) {
            prefixFileSaved = in.readString();
            pathFileSaved = in.readString();
        }

        @Override
        public String toString() {
            return getPrefixFileSaved() + " > " + getPathFileSaved();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(prefixFileSaved);
            dest.writeString(pathFileSaved);
        }

        public String getPrefixFileSaved() {
            return prefixFileSaved;
        }

        public String getPathFileSaved() {
            return pathFileSaved;
        }
    }


}
