package com.alangeorge.android.hermes.model;

public class ModelException extends Exception {
    public ModelException(String message, Exception e) {
        super(message, e);
    }

    public ModelException(String s) {
        super(s);
    }
}
