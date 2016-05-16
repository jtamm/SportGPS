package com.example.androidstudio.sportgps;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by AndroidStudio on 15.05.2016.
 */
public class StatisticParcelable implements Parcelable {
    public int cResetDistance;
    public int wayPointDistance;
    public int totalDistance;
    public int cResetLines;
    public int wayPointLines;
    public int totalLines;
    public int wayPointCount;
    public int currentSpeed;
    public LatLng currentPoint;
    public long timeSeconds;
    public StatisticParcelable(){}


    protected StatisticParcelable(Parcel in) {
        cResetDistance = in.readInt();
        wayPointDistance = in.readInt();
        totalDistance = in.readInt();
        cResetLines = in.readInt();
        wayPointLines = in.readInt();
        totalLines = in.readInt();
        wayPointCount = in.readInt();
        currentSpeed = in.readInt();
        currentPoint = in.readParcelable(LatLng.class.getClassLoader());
        timeSeconds = in.readLong();
    }

    public static final Creator<StatisticParcelable> CREATOR = new Creator<StatisticParcelable>() {
        @Override
        public StatisticParcelable createFromParcel(Parcel in) {
            return new StatisticParcelable(in);
        }

        @Override
        public StatisticParcelable[] newArray(int size) {
            return new StatisticParcelable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(cResetDistance);
        dest.writeInt(wayPointDistance);
        dest.writeInt(totalDistance);
        dest.writeInt(cResetLines);
        dest.writeInt(wayPointLines);
        dest.writeInt(totalLines);
        dest.writeInt(wayPointCount);
        dest.writeInt(currentSpeed);
        dest.writeParcelable(currentPoint, flags);
        dest.writeLong(timeSeconds);
    }
}
