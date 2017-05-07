package fm.kirtsim.kharos.animations;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatTextView;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;

/**
 *
 * Created by kharos on 02/05/2017.
 */

public class Number extends AppCompatTextView {
    private GestureDetector gestureDetector;
    private NumberClickListener clickListener;
    private int index;

    interface NumberClickListener {
        void onFingerDown();
        void onFingerUp();
        void onNumberClicked(int value, int index, Number number);
    }

    public Number(Context context, NumberClickListener clickListener, int index) {
        super(context);
        this.gestureDetector = new GestureDetector(getContext(), new TouchDetector(this));
        this.clickListener = clickListener;
        this.index = index;
    }

    public Number(Context context) {
        super(context);
    }

    public Number(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Number(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public int getNumber() {
        return Integer.parseInt(getText().toString());
    }

    public int getIndex() {
        return this.index;
    }

    private void onTapped() {
        clickListener.onNumberClicked(getNumber(), index, this);
    }

    private void onFingerDown() {
        clickListener.onFingerDown();
    }

    private void onFingerUp() {
        clickListener.onFingerUp();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private static class TouchDetector extends GestureDetector.SimpleOnGestureListener {
        private WeakReference<Number> listener;

        TouchDetector(Number listener) {
            super();
            this.listener = new WeakReference<>(listener);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            listener.get().onFingerDown();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            listener.get().onFingerUp();
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            listener.get().onTapped();
            return false;
        }
    }
}
