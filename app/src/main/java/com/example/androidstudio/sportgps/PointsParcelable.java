package com.example.androidstudio.sportgps;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by AndroidStudio on 16.05.2016.
 */
public class PointsParcelable implements Parcelable {
    public List<LatLng> totalPointList;
    public List<LatLng> wayPointList;
    public List<LatLng> cResetPointList;

    public PointsParcelable(){};

    protected PointsParcelable(Parcel in) {
        totalPointList = in.createTypedArrayList(LatLng.CREATOR);
        wayPointList = in.createTypedArrayList(LatLng.CREATOR);
        cResetPointList = in.createTypedArrayList(LatLng.CREATOR);
    }

    public static final Creator<PointsParcelable> CREATOR = new Creator<PointsParcelable>() {
        @Override
        public PointsParcelable createFromParcel(Parcel in) {
            return new PointsParcelable(in);
        }

        @Override
        public PointsParcelable[] newArray(int size) {
            return new PointsParcelable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(totalPointList);
        dest.writeTypedList(wayPointList);
        dest.writeTypedList(cResetPointList);
    }
}
