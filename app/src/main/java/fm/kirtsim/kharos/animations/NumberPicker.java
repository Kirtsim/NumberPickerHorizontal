package fm.kirtsim.kharos.animations;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/**
 *
 * Created by kharos on 24/04/2017.
 */

public class NumberPicker extends HorizontalScrollView implements Number.NumberClickListener{
    public static final String TAG = HorizontalScrollView.class.getSimpleName();
    private static final int SCROLL_LEFT = 0;
    private static final int SCROLL_RIGHT = 1;
    private final int TEXT_VIEW_WIDTH_DP = 60;

    private int min, max;
    private LinearLayout container;
    private Number selectedNumber;

    private int initialScrollX;
    private int scrollDirection;
    private int textViewWidth;
    private float textSizeSP;
    private boolean isScrolling;
    private boolean fingerDown;


    public NumberPicker(Context context) {
        super(context);
        initialize(context);
    }

    public NumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        min = 1;
        max = 50;
        textSizeSP = 25;
        this.setHorizontalScrollBarEnabled(false);
        initializeContainers(context);
        this.addView(container);
        populateWithNumbers();
        selectedNumber = (Number) container.getChildAt(1);
    }

    private void initializeContainers(Context context) {
        container = new LinearLayout(context);
        container.setLayoutParams(createContainerLayoutParams());
        container.setOrientation(LinearLayout.HORIZONTAL);
    }

    private void populateWithNumbers() {
        final int viewWidth = (int) densityToPixels(TEXT_VIEW_WIDTH_DP, getContext());
        int index = 0;
        for (int number = min; number <= max; number++)
            container.addView(createNumber(number, index++, viewWidth));
    }

    private Number createNumber(final int number, final int index, final int width) {
        Number numberTV = new Number(getContext(), this, index);
        numberTV.setLayoutParams(createLayoutParams(width));
        numberTV.setText(String.valueOf(number));
        numberTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        numberTV.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        numberTV.setTextColor(Color.WHITE);
        return numberTV;
    }



    private void setMax(int max) {
        this.max = max;
        if (max < min)
            setMin(max);
    }

    public void setMin(int min) {
        this.min = min;
        if (min > max)
            setMax(min);
    }

    public void setNumberWidthInDP(int width) {
        this.textViewWidth = (int) densityToPixels(width, getContext());
        updateNumbersWidth(width);
    }




    private void updateNumbersWidth(final int width) {
        final int numberCount = container.getChildCount() - 1;
        for (int i = 1; i < numberCount; ++i) {
            TextView number = (TextView) container.getChildAt(i);
            ViewGroup.LayoutParams params = number.getLayoutParams();
            params.width = width;
            number.setLayoutParams(params);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            final int textViewWidth = (int) densityToPixels(TEXT_VIEW_WIDTH_DP, getContext());
            insertSpaces(w, textViewWidth);
            this.setBackground(createNewBackground(w, h));
        }
    }

    private void insertSpaces(final int viewWidth, final int textViewWidth) {
        final int spaceWidth = viewWidth / 2 - (textViewWidth / 2);
        final Space startSpace = createSpace(spaceWidth);
        final Space endSpace = createSpace(spaceWidth);
        container.addView(startSpace, 0);
        container.addView(endSpace);
    }

    private Space createSpace(final int spaceWidth) {
        Space space = new Space(getContext());
        space.setLayoutParams(createLayoutParams(spaceWidth));
        return space;
    }

    private Drawable createNewBackground(final int viewWidth, final int viewHeight) {
        Background background = new Background(this, viewWidth, viewHeight);
        background.setBackgroundColor(Color.BLUE);
        background.setForegroundColor(Color.WHITE);
        return background;
    }



    private FrameLayout.LayoutParams createContainerLayoutParams() {
        final int height =  (int) densityToPixels(TEXT_VIEW_WIDTH_DP, getContext());
        return new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, height);
    }

    private LinearLayout.LayoutParams createLayoutParams(int width) {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        return params;
    }

    private static float densityToPixels(int dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }





    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN)
            onFingerDown();
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            Log.d(TAG, "actionUP");
            fingerDown = false;
            if (!isScrolling)
                onScrollEnd(getScrollX());
        } else
            fingerDown = true;
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        scrollDirection = l > oldl ? SCROLL_RIGHT : SCROLL_LEFT;
        if (!isScrolling) {
            isScrolling = true;
            checkEndOfScroll();
        }
    }

    private void checkEndOfScroll() {
        final int currentScrollX = getScrollX();
        if (currentScrollX != initialScrollX) {
            initialScrollX = currentScrollX;
            scheduleScrollEndCheck();
        } else {
//            Log.d(TAG, "setting isScrolling to false");
            isScrolling = false;
            if (!fingerDown)
                onScrollEnd(currentScrollX);
        }
    }

    private void scheduleScrollEndCheck() {
        final long DELAY = 50;
        this.postDelayed(this::checkEndOfScroll, DELAY);
    }

    private void onScrollEnd(int currentScrollX) {
        settleToSelection(currentScrollX);
        animateNumberBeingSelected();
    }

    private void settleToSelection(final int currentScroll) {
        final int index = calculateNumberIndex(currentScroll);
        selectedNumber = (Number) container.getChildAt(index + 1);
        final int numberWidth = selectedNumber.getWidth();
        this.post(() -> smoothScrollTo(index * numberWidth, 0));
    }

    private int calculateNumberIndex(final int currentScroll) {
        final int numberWidth = selectedNumber.getWidth();
        double numberIndex = currentScroll / (numberWidth * 1.0f);
        double indexWhole = ((int) numberIndex) + 0.45;
        final boolean isInMiddle = numberIndex > indexWhole && numberIndex < (indexWhole + 0.1f);
        if (isInMiddle && scrollDirection == SCROLL_LEFT)
            numberIndex = Math.floor(numberIndex);
        else
            numberIndex = Math.round(numberIndex);
        return (int) numberIndex;
    }

    private void deselectNumber() {
        Log.d(TAG, "deselecting");
        selectedNumber.setTextSize(textSizeSP);
        selectedNumber.setTypeface(null, Typeface.NORMAL);
    }

    private void applySelection() {
        selectedNumber.setTextSize(textSizeSP + 6.0f);
        selectedNumber.setTypeface(null, Typeface.BOLD);
    }

    @Override
    public void onFingerDown() {
        deselectNumber();
        fingerDown = true;
    }

    @Override
    public void onNumberClicked(int value, int index, Number number) {
        int toScroll = (index * number.getWidth()) - getScrollX();
        this.post(() -> smoothScrollBy(toScroll, 0));
    }

    private void animateNumberBeingSelected() {
//        ValueAnimator animator = createAnimatorForNumberSize(10.0f, 100);
//        animator.addListener(createAnimListenerToBoldNumber());
//        animator.start();
//        Log.d(TAG, "selecting");
        selectedNumber.setTextSize(textSizeSP + 6.0f);
        selectedNumber.setTypeface(null, Typeface.BOLD);
    }


    private ValueAnimator createAnimatorForNumberSize(float sizeChange, int duration) {
        final float textSize = selectedNumber.getTextSize();
        ValueAnimator animator = ValueAnimator.ofFloat(textSize, textSize + sizeChange);
        animator.setDuration(duration);
        animator.addUpdateListener(a -> selectedNumber.
                setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) a.getAnimatedValue()));
        return animator;
    }

    private Animator.AnimatorListener createAnimListenerToBoldNumber() {
        return new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override public void onAnimationStart(Animator animation) {
//                selectedNumber.setTypeface(null, Typeface.BOLD);
            }
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
        };
    }

    /**
     *                             .:*******:.
     * .:**************************** CLASS ******************************:.
     * #~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#
     * #                            BACKGROUND                             #
     * #~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#~#
     */
    private static class Background extends Drawable {
        private WeakReference<NumberPicker> numberPickerWR;
        private Paint paint;
        private int backgroundColor;
        private int foregroundColor;
        private int viewWidth;
        private int viewHeight;

        Background(NumberPicker numberPicker, int viewWidth, int viewHeight) {
            this.numberPickerWR = new WeakReference<>(numberPicker);
            this.viewWidth = viewWidth;
            this.viewHeight = viewHeight;
            this.paint = new Paint();
            this.paint.setStrokeWidth(densityToPixels(3, numberPicker.getContext()));
            setPaintForBackground();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            NumberPicker np = numberPickerWR.get();
            final float textViewWidthHalf = densityToPixels(np.TEXT_VIEW_WIDTH_DP, np.getContext()) / 2;
            final float viewWidthHalf = viewWidth / 2.0f;
            final float left = viewWidthHalf - textViewWidthHalf;
            final float right = viewWidthHalf + textViewWidthHalf;
            final float bottom = viewWidth - paint.getStrokeWidth();
            final float top = 0;
            setPaintForBackground();
            canvas.drawRect(0, 0, viewWidth, viewHeight, paint);

            setPaintForForeground();
            canvas.drawRect(left, top, right, bottom, paint);
        }


        private void setPaintForBackground() {
            paint.setColor(backgroundColor);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
        }

        private void setPaintForForeground() {
            paint.setColor(foregroundColor);
            paint.setStyle(Paint.Style.STROKE);
        }

        void setBackgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        void setForegroundColor(int foregroundColor) {
            this.foregroundColor = foregroundColor;
        }

        @Override public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
            paint.setAlpha(alpha);
        }

        @Override public void setColorFilter(@Nullable ColorFilter colorFilter) {}

        @Override public int getOpacity() { return PixelFormat.UNKNOWN; }

    }
}
