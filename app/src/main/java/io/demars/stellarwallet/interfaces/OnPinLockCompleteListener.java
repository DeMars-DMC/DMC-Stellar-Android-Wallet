package io.demars.stellarwallet.interfaces;

import org.jetbrains.annotations.NotNull;

import io.demars.stellarwallet.views.pin.PinLockView;

public abstract class OnPinLockCompleteListener implements PinLockView.PinLockListener {
    @Override
    public void onEmpty() {
        // empty
    }

    @Override
    public void onPinChange(int pinLength, @NotNull String intermediatePin) {
        // empty
    }
}
