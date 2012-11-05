package com.facebook.internal;

import com.facebook.Request;
import com.facebook.RequestBatch;

public class CacheableRequestBatch extends RequestBatch {
    private String cacheKey;
    private boolean forceRoundTrip;

    public CacheableRequestBatch() {
    }

    public CacheableRequestBatch(Request... requests) {
        super(requests);
    }

    public final String getCacheKeyOverride() {
        return cacheKey;
    }

    // If this is set, the provided string will override the default key (the URL) for single requests.
    // There is no default for multi-request batches, so no caching will be done unless the override is
    // specified.
    public final void setCacheKeyOverride(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public final boolean getForceRoundTrip() {
        return forceRoundTrip;
    }

    public final void setForceRoundTrip(boolean forceRoundTrip) {
        this.forceRoundTrip = forceRoundTrip;
    }

}
