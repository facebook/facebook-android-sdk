package com.facebook;

interface GraphObjectCursor<T extends GraphObject> {
    boolean isFromCache();

    boolean areMoreObjectsAvailable();

    int getCount();

    int getPosition();

    boolean move(int offset);

    boolean moveToPosition(int position);

    boolean moveToFirst();

    boolean moveToLast();

    boolean moveToNext();

    boolean moveToPrevious();

    boolean isFirst();

    boolean isLast();

    boolean isBeforeFirst();

    boolean isAfterLast();

    T getGraphObject();

    void close();

    boolean isClosed();
}
