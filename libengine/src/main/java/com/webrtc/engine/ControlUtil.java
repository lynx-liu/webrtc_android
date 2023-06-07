package com.webrtc.engine;

import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ControlUtil  {
    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT = 1;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2;

    private static final int DEVICE_ID_VIRTUAL = -1;
    private long lastTouchDown;
    private Point screenSize = new Point();
    private final InputManager inputManager;
    private final PointersState pointersState = new PointersState();
    private final MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[PointersState.MAX_POINTERS];
    private final MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[PointersState.MAX_POINTERS];

    public ControlUtil(final Context context){
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display display = displayManager.getDisplay(0);
        display.getRealSize(screenSize);

        inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);

        for (int i = 0; i < PointersState.MAX_POINTERS; ++i) {
            MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
            props.toolType = MotionEvent.TOOL_TYPE_FINGER;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.orientation = 0;
            coords.size = 1;

            pointerProperties[i] = props;
            pointerCoords[i] = coords;
        }
    }

    public Point getScreenSize() {
        return screenSize;
    }

    public boolean injectTouch(int action, final long pointerId, final Point point, final float pressure, final int buttons) {
        int pointerIndex = pointersState.getPointerIndex(pointerId,action==MotionEvent.ACTION_DOWN);
        if (pointerIndex < 0){
            return true;
        }

        Pointer pointer = pointersState.get(pointerIndex);
        pointer.setPoint(point);
        pointer.setPressure(pressure);
        pointer.setUp(action == MotionEvent.ACTION_UP);

        int pointerCount = pointersState.update(pointerProperties, pointerCoords);

        if (pointerCount == 1) {
            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchDown = SystemClock.uptimeMillis();
            }
        } else {
            // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
            if (action == MotionEvent.ACTION_UP) {
                action = MotionEvent.ACTION_POINTER_UP | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            } else if (action == MotionEvent.ACTION_DOWN) {
                action = MotionEvent.ACTION_POINTER_DOWN | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            }
        }

        MotionEvent event = MotionEvent
                .obtain(lastTouchDown, SystemClock.uptimeMillis(), action, pointerCount, pointerProperties, pointerCoords, 0, buttons, 1f, 1f, DEVICE_ID_VIRTUAL, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);
        return injectInputEvent(event, INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public boolean injectTouchForMouse(final int action, final long lastDownTime, final long eventTime, final Point point) {
        MotionEvent.PointerProperties props = pointerProperties[0];
        props.clear();
        props.id = 0;
        props.toolType = MotionEvent.TOOL_TYPE_FINGER;

        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.clear();
        coords.size = 1;
        coords.x = point.x;
        coords.y = point.y;

        MotionEvent event = MotionEvent
                .obtain(lastDownTime, eventTime, action, 1, pointerProperties, pointerCoords, 0, 0, 1f, 1f, DEVICE_ID_VIRTUAL, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);
        return injectInputEvent(event, INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public boolean injectMouse(final int action, final long lastDownTime, final long eventTime, final Point point, final int buttons) {
        MotionEvent.PointerProperties props = pointerProperties[0];
        props.clear();
        props.id = 0;
        props.toolType = MotionEvent.TOOL_TYPE_MOUSE;

        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.clear();
        coords.size = 1;
        coords.x = point.x;
        coords.y = point.y;

        MotionEvent event = MotionEvent
                .obtain(lastDownTime, eventTime, action, 1, pointerProperties, pointerCoords, 0, buttons, 1f, 1f, DEVICE_ID_VIRTUAL, 0,
                        InputDevice.SOURCE_MOUSE, 0);
        return injectInputEvent(event, INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public boolean injectScroll(final Point point, final long lastDownTime, final long eventTime, final int hScroll, final int vScroll) {
        MotionEvent.PointerProperties props = pointerProperties[0];
        props.clear();
        props.id = 0;

        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.clear();
        coords.x = point.x;
        coords.y = point.y;
        coords.setAxisValue(MotionEvent.AXIS_HSCROLL, hScroll);
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);

        MotionEvent event = MotionEvent
                .obtain(lastDownTime, eventTime, MotionEvent.ACTION_SCROLL, 1, pointerProperties, pointerCoords, 0, 0, 1f, 1f, DEVICE_ID_VIRTUAL, 0,
                        InputDevice.SOURCE_MOUSE, 0);
        return injectInputEvent(event, INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public boolean injectKeycode(final int keyCode) {
        return injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode, 0, 0) && injectKeyEvent(KeyEvent.ACTION_UP, keyCode, 0, 0);
    }

    public boolean injectKeyEvent(int action, int keyCode, int repeat, int metaState) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, action, keyCode, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        return injectInputEvent(event, INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public boolean injectTowKeycode(final int keyCode1, final int KeyCode2) {
        long now = SystemClock.uptimeMillis();
        boolean ret = injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode1, now, 0, 0, INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)
                && injectKeyEvent(KeyEvent.ACTION_DOWN, KeyCode2, now, 0, 0, INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);

        long eventTime =  SystemClock.uptimeMillis();
        return injectKeyEvent(KeyEvent.ACTION_UP, keyCode1, eventTime, 0 ,0, INJECT_INPUT_EVENT_MODE_ASYNC)
                && injectKeyEvent(KeyEvent.ACTION_UP, KeyCode2, eventTime, 0 ,0, INJECT_INPUT_EVENT_MODE_ASYNC) && ret;
    }

    public boolean injectKeyEvent(final int action, final int keyCode, final long time, final int repeat, final int metaState, final int mode) {
        KeyEvent event = new KeyEvent(time, time, action, keyCode, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        return injectInputEvent(event, mode);
    }

    public boolean injectInputEvent(final InputEvent inputEvent, final int mode) {
        try {
            Method method = inputManager.getClass().getMethod("injectInputEvent", InputEvent.class, int.class);
            return (boolean) method.invoke(inputManager, inputEvent, mode);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            Log.e("llx",e.toString());
            return false;
        }
    }
}
