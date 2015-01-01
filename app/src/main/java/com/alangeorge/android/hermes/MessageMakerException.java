package com.alangeorge.android.hermes;

public class MessageMakerException extends Exception {
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "Hermes.MessageMakerException";

    public MessageMakerException(Exception e) {
        super(e);
    }
}
