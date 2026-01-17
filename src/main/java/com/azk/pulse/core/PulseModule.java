package com.azk.pulse.core;

public interface PulseModule {
    String getName();

    void enable();

    void disable();

    boolean isEnabled();

    void setEnabled(boolean enabled);
}
