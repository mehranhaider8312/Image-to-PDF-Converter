package com.mehran.imagetopdfconverter;

import android.net.Uri;

public class Picture {

    private Uri imageUri;

    public Picture() {
    }

    public Picture(Uri uri) {
        this.imageUri = uri;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri uri) {
        this.imageUri = uri;
    }
}
