package com.smartlib.feature;

import com.smartlib.domain.Feature;

public class FeatureDisabledException extends RuntimeException {
    private final Feature feature;
    public FeatureDisabledException(Feature feature) {
        super(feature.getLabel() + " is not enabled for this library");
        this.feature = feature;
    }
    public Feature getFeature() { return feature; }
}
