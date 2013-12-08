package com.theodoorthomas.android.memoryloss;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class MainThreadBus extends Bus {
    private static Bus _bus;
    private Handler _handler = new Handler(Looper.getMainLooper());

    public MainThreadBus() {
        if (_bus == null) {
        	Log.w("MemoryLossBuss", "Creating new communication bus.");
            _bus = new Bus(ThreadEnforcer.ANY);
        }
    }

    @Override public void register(Object obj) {
        _bus.register(obj);
    }

    @Override public void unregister(Object obj) {
        _bus.unregister(obj);
    }

    @Override public void post(final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            _bus.post(event);
        } else {
            _handler.post(new Runnable() {
                @Override public void run() {
                    _bus.post(event);
                }
            });
        }
    }
}