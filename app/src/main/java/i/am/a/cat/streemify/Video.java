package i.am.a.cat.streemify;

import android.os.Parcel;
import android.os.Parcelable;

public class Video implements Parcelable {
    private final String thumbnail;
    private final String name;
    private final String path; // Path যেখানে ভিডিওটি অবস্থিত
    private final long duration; // Updated to long for better accuracy
    private final long dateAdded;
    private final String resolution;
    private final long size; // Updated to long for size in bytes

    // Constructor
    public Video(String thumbnail, String name, String path, long duration, long dateAdded, String resolution, long size) {
        this.thumbnail = thumbnail;
        this.name = name;
        this.path = path;
        this.duration = duration;
        this.dateAdded = dateAdded;
        this.resolution = resolution;
        this.size = size;
    }

    // Parcelable constructor
    protected Video(Parcel in) {
        thumbnail = in.readString();
        name = in.readString();
        path = in.readString();
        duration = in.readLong();
        dateAdded = in.readLong();
        resolution = in.readString();
        size = in.readLong();
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel in) {
            return new Video(in);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(thumbnail);
        dest.writeString(name);
        dest.writeString(path);
        dest.writeLong(duration);
        dest.writeLong(dateAdded);
        dest.writeString(resolution);
        dest.writeLong(size);
    }

    // Getter methods
    public String getThumbnail() {
        return thumbnail;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getDuration() {
        return duration;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public String getResolution() {
        return resolution;
    }

    public long getSize() {
        return size;
    }
}