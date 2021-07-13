package com.therearenotasksforus.videohostingapi.bucket;

public enum BucketName {
    PROFILE_IMAGE("videohostingapibucket");

    private final String bucketName;

    BucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBucketName() {
        return bucketName;
    }
}
