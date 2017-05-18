package fm.kirtsim.kharos.animations;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
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

import java.lang.ref.WeakReference;


/**
 *
 * Created by kharos on 24/04/2017.
 */

public class NumberPicker extends HorizontalScrollView implements Number.NumberClickListener{
    public static final String TAG = HorizontalScrollView.class.getSimpleName();
    private static final int SCROLL_LEFT = 0;
    private static final int SCROLL_RIGHT = 1;
    private static final int NUMBER_START_INDEX = 1;
    private static final float FONT_SIZE_ADDITION = 6.0f;
    private int text_view_width_px = 60;

    private int min, max;
    private LinearLayout container;
    private Background background;
    private Number selectedNumber;

    private int initialScrollX;
    private int scrollDirection;
    private float textSizeSP;
    private boolean isScrolling;
    private boolean fingerDown;

    public NumberPicker(Context context) {
        this(context, null, 0);
    }

    public NumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params != null && params.width == ViewGroup.LayoutParams.WRAP_CONTENT)
            throw new IllegalArgumentException("Horizontal Number Picker's width must not be 'wrap_content'");
        super.setLayoutParams(params);
    }

    private void initialize(Context context) {
        min = 1;
        max = 3;
        textSizeSP = 25;
        this.setHorizontalScrollBarEnabled(false);
        initializeTextViewWidth(context);
        initializeContainers(context);
        insertSpaces(context);
        addNumbersUpToMax(min);
        selectedNumber = (Number) container.getChildAt(NUMBER_START_INDEX);
        applySelection();
    }

    private void initializeTextViewWidth(Context context) {
        Rect bounds = getTextBoundsOfMax(context);
        final int calculatedWidth = (bounds.width() + bounds.width() / 2);
        final int minDefault = (int) defaultDimensionFromFontSize(textSizeSP, getContext());
        text_view_width_px = Math.max(minDefault, calculatedWidth);
    }

    private Rect getTextBoundsOfMax(Context context) {
        Rect bounds = new Rect();
        Paint paint = new Paint();
        String text = String.valueOf(max);
        paint.setTextSize(spToPixels((int) (textSizeSP + FONT_SIZE_ADDITION), context));
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds;
    }

    private void initializeContainers(Context context) {
        container = new LinearLayout(context);
        container.setLayoutParams(createContainerLayoutParams());
        container.setOrientation(LinearLayout.HORIZONTAL);
        this.addView(container);
    }

    private void insertSpaces(final Context context) {
        final Space startSpace = new Space(context);
        final Space endSpace = new Space(context);
        startSpace.setLayoutParams(new ViewGroup.LayoutParams(0,0));
        endSpace.setLayoutParams(new ViewGroup.LayoutParams(0,0));
        container.addView(startSpace, 0);
        container.addView(endSpace);
    }



    public int getNumberCount() {
        return container.getChildCount() - 2;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        if (max >= min)
            this.max = max;
    }

    public void setMin(int min) {
        if (min <= max)
            this.min = min;
    }

    public void applyBoundaryChanges() {
        applyLowerBoundary();
        applyHigherBoundary();
    }

    private void applyLowerBoundary() {
        final int lowestNumber = ((Number) container.getChildAt(1)).getNumber();
        if (min < lowestNumber) {
            addNumbersToFront(lowestNumber);
            post(() -> scrollBy(text_view_width_px * (lowestNumber - min), 0)); // with last number
        }                   //selected it blinks (appears for a moment) to its right before scrolled
        else if (min > lowestNumber) {
            final int removeCount = min - lowestNumber;
            final int overrideCount = getNumberCount() - removeCount;
            overrideNumbers(NUMBER_START_INDEX, min, overrideCount);
            removeNumbers(removeCount);
        }
    }

    private void addNumbersToFront(final int stopNumber) {
        if (stopNumber < min)
            return;

        final int insertCount = stopNumber - min;
        ensureNumberCapacity(insertCount);
        overrideNumbers(NUMBER_START_INDEX, min, max - min + 1);
    }

    private void overrideNumbers(int startIndex, int startNumber, final int count) {
        if (startIndex + count > container.getChildCount() - 1)
            throw new IndexOutOfBoundsException();
        for (int i = 0; i < count; ++i, ++startIndex, ++startNumber) {
            Number number = (Number) container.getChildAt(startIndex);
            number.setNumber(startNumber);
        }
    }

    private void applyHigherBoundary() {
        final int maxNumber = ((Number) container.getChildAt(getLastNumberIndex())).getNumber();
        if (maxNumber < max)
            addNumbersUpToMax(maxNumber+1);
        else
            removeNumbers(maxNumber - max);
    }

    private void removeNumbers(final int count) {
        final int lastIndex = getLastNumberIndex();
        final int removeFromIndex = lastIndex - count + 1;
        if (removeFromIndex <= NUMBER_START_INDEX)
            throw new IllegalArgumentException("Trying to remove too many numbers");
        container.removeViews(removeFromIndex, count);
    }


    private void addNumbersUpToMax(int startNumber) {
        int index = getLastNumberIndex() + 1;
        while (startNumber <= max) {
            Number num = createNumber(startNumber++, index);
            container.addView(num, index++);
        }
    }

    private void ensureNumberCapacity(int count) {
        int index = getLastNumberIndex() + 1;
        while (count-- > 0) {
            Number num = createNumber(max, index);
            container.addView(num, index++);
        }
    }

    private int getLastNumberIndex() {
        return container.getChildCount() - 2;
    }

    private Number createNumber(final int number, final int index) {
        Number numberTV = new Number(getContext(), this, index);
        numberTV.setLayoutParams(createLayoutParams(text_view_width_px));
        numberTV.setText(String.valueOf(number));
        numberTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        numberTV.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        numberTV.setTextColor(Color.WHITE);
        return numberTV;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            inflateSpaces(w, text_view_width_px);
            this.setBackground(initializeNewBackground(w, h));
        }
    }


    private void inflateSpaces(final int viewWidth, final int textViewWidth) {
        final int spaceWidth = viewWidth / 2 - (textViewWidth / 2);
        final Space startSpace = (Space) container.getChildAt(0);
        final Space endSpace = (Space) container.getChildAt(container.getChildCount() - 1);
        startSpace.setLayoutParams(createLayoutParams(spaceWidth));
        endSpace.setLayoutParams(createLayoutParams(spaceWidth));
    }

    private Drawable initializeNewBackground(final int viewWidth, final int viewHeight) {
        Background background = new Background(this, viewWidth, viewHeight);
        background.setBackgroundColor(Color.BLUE);
        background.setForegroundColor(Color.WHITE);
        this.background = background;
        return background;
    }



    private FrameLayout.LayoutParams createContainerLayoutParams() {
        final float height = defaultDimensionFromFontSize(textSizeSP, getContext());
        return new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, (int) height);
    }

    private LinearLayout.LayoutParams createLayoutParams(int width) {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        return params;
    }

    private static float defaultDimensionFromFontSize(final float fontSizeSP, final Context context) {
        return spToPixels((int) ((fontSizeSP + FONT_SIZE_ADDITION) * 2), context);
    }

    private static float densityToPixels(int dp, Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    private static float spToPixels(int sp, Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics);
    }





    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN)
            onFingerDown();
        else if (ev.getAction() == MotionEvent.ACTION_UP) {
            fingerDown = false;
            if (!isScrolling)
                onScrollEnd(getScrollX());
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        scrollDirection = l > oldl ? SCROLL_RIGHT : SCROLL_LEFT;
        if (!isScrolling) {
            isScrolling = true;
            deselectNumber();
            checkEndOfScroll();
        }
    }

    private void checkEndOfScroll() {
        final int currentScrollX = getScrollX();
        if (currentScrollX != initialScrollX) {
            initialScrollX = currentScrollX;
            scheduleScrollEndCheck();
        } else {
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
        if (settleToSelection(currentScrollX))
            applySelection();
    }

    private boolean settleToSelection(final int currentScroll) {
        final int relIndex = calculateRelativeNumberIndex(currentScroll);
        selectedNumber = (Number) container.getChildAt(relIndex + 1);
        final int scrollTo = relIndex * selectedNumber.getWidth();
        if (scrollTo != currentScroll) {
            this.post(() -> smoothScrollTo(scrollTo, 0));
            return false;
        }
        return true;
    }

    private int calculateRelativeNumberIndex(final int currentScroll) {
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
        selectedNumber.setTextSize(textSizeSP);
        selectedNumber.setTypeface(null, Typeface.NORMAL);
    }

    private void applySelection() {
        selectedNumber.setTextSize(textSizeSP + FONT_SIZE_ADDITION);
        selectedNumber.setTypeface(null, Typeface.BOLD);
    }

    @Override
    public void onFingerUp() {
        fingerDown = false;
    }

    @Override
    public void onFingerDown() {
        fingerDown = true;
    }

    @Override
    public void onNumberClicked(int value, int index, Number number) {
        fingerDown = false;
        if (number != selectedNumber) {
            int toScroll = ((index-1) * number.getWidth()) - getScrollX();
            this.post(() -> smoothScrollBy(toScroll, 0));
        }
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
            this.backgroundColor = Color.argb(0, 0, 0, 0);
            this.foregroundColor = Color.BLACK;
            this.paint = new Paint();
            this.paint.setStrokeWidth(densityToPixels(3, numberPicker.getContext()));
            setPaintForBackground();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            NumberPicker np = numberPickerWR.get();
            final float textViewWidthHalf = np.text_view_width_px / 2;
            final float viewWidthHalf = viewWidth / 2.0f;

            final float left = viewWidthHalf - textViewWidthHalf;
            final float right = viewWidthHalf + textViewWidthHalf;
            final float bottom = viewHeight;
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
