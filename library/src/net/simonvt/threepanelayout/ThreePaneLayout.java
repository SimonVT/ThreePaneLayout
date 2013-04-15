package net.simonvt.threepanelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

public class ThreePaneLayout extends ViewGroup {

    static final boolean USE_TRANSLATIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    private static final boolean DEBUG = false;

    private static final int DURATION_MAX = DEBUG ? 10000 : 800;

    private static final int INDICATOR_ANIM_DURATION = 600;

    public static final int PANE_LEFT = 1;

    public static final int PANE_MIDDLE = 2;

    public static final int PANE_RIGHT = 4;

    private static final int DEFAULT_DROP_SHADOW_WIDTH_DP = 6;

    /**
     * The time between each frame.
     */
    protected static final int ANIMATION_DELAY = 1000 / 60;

    /**
     * State when the layout is not animating and the left pane is visible.
     */
    public static final int STATE_LEFT_VISIBLE = 0;

    /**
     * State when the layout is animating to the right pane.
     */
    public static final int STATE_ANIMATE_RIGHT = 1;

    /**
     * State when the layout is animating to the left pane.
     */
    public static final int STATE_ANIMATE_LEFT = 2;

    /**
     * State when the layout is not animating and the right pane is visible.
     */
    public static final int STATE_RIGHT_VISIBLE = 4;

    private static final Interpolator SMOOTH_INTERPOLATOR = new SmoothInterpolator();

    private static final Interpolator INDICATOR_INTERPOLATOR = new AccelerateInterpolator();

    private FrameLayout mLeftPane;

    private FrameLayout mMiddlePane;

    private FrameLayout mRightPane;

    private float mOffset;

    private boolean mLayerTypeHardware;

    private FloatScroller mScroller;

    private final Runnable mDragRunnable = new Runnable() {
        @Override
        public void run() {
            postAnimationInvalidate();
        }
    };

    private Drawable mShadow;

    private int mDropShadowWidth;

    private boolean mMiddlePaneCollapsible;

    private int mLeftPaneWidth;

    private int mMiddlePaneCollapsedWidth;

    private int mMiddlePaneExpandedWidth;

    private int mVisiblePanes = PANE_LEFT | PANE_MIDDLE;

    private OnPaneStateChangeListener mPaneStateChangeListener;

    private int mPageState = STATE_LEFT_VISIBLE;

    private View mLeftActiveView;

    private Bitmap mLeftActiveIndicator;

    private final Rect mLeftActiveRect = new Rect();

    private int mLeftActivePosition;

    private int mLeftIndicatorStartPos;

    private int mLeftIndicatorTop;

    private float mLeftIndicatorOffset;

    private final FloatScroller mLeftIndicatorScroller = new FloatScroller(SMOOTH_INTERPOLATOR);

    private final Runnable mLeftIndicatorRunnable = new Runnable() {
        @Override
        public void run() {
            animateLeftIndicatorInvalidate();
        }
    };

    private boolean mLeftIndicatorAnimating;

    private View mMiddleActiveView;

    private Bitmap mMiddleActiveIndicator;

    private final Rect mMiddleActiveRect = new Rect();

    private int mMiddleActivePosition;

    private int mMiddleIndicatorStartPos;

    private int mMiddleIndicatorTop;

    private float mMiddleIndicatorOffset;

    private final FloatScroller mMiddleIndicatorScroller = new FloatScroller(SMOOTH_INTERPOLATOR);

    private final Runnable mMiddleIndicatorRunnable = new Runnable() {
        @Override
        public void run() {
            animateMiddleIndicatorInvalidate();
        }
    };

    private boolean mMiddleIndicatorAnimating;

    private ViewTreeObserver.OnScrollChangedListener mScrollChangedListener
            = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            if (mLeftActiveView != null || mMiddleActiveView != null) {
                invalidate();
            }
        }
    };

    public interface OnPaneStateChangeListener {

        void onPaneStateChange(int oldState, int newState);
    }

    public ThreePaneLayout(Context context) {
        this(context, null);
    }

    public ThreePaneLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThreePaneLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mScroller = new FloatScroller(SMOOTH_INTERPOLATOR);

        mLeftPane = new BuildLayerFrameLayout(context);
        addView(mLeftPane, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        mMiddlePane = new BuildLayerFrameLayout(context);
        addView(mMiddlePane, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        mRightPane = new BuildLayerFrameLayout(context);
        addView(mRightPane, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ThreePaneLayout, R.attr.threePaneLayoutStyle,
                defStyle);

        final int leftPaneLayout = a.getResourceId(R.styleable.ThreePaneLayout_leftPaneLayout, -1);
        if (leftPaneLayout != -1) {
            setLeftPaneLayout(leftPaneLayout);
        }
        final int middlePaneLayout = a.getResourceId(R.styleable.ThreePaneLayout_middlePaneLayout, -1);
        if (middlePaneLayout != -1) {
            setMiddlePaneLayout(middlePaneLayout);
        }
        final int rightPaneLayout = a.getResourceId(R.styleable.ThreePaneLayout_rightPaneLayout, -1);
        if (rightPaneLayout != -1) {
            setRightPaneLayout(rightPaneLayout);
        }

        mLeftPaneWidth = a.getDimensionPixelSize(R.styleable.ThreePaneLayout_leftPaneWidth, dpToPx(250));

        mMiddlePaneCollapsible = a.getBoolean(R.styleable.ThreePaneLayout_middlePaneCollapsible, true);

        mMiddlePaneCollapsedWidth = a.getDimensionPixelSize(R.styleable.ThreePaneLayout_middlePaneCollapsedWidth,
                dpToPx(450));

        final int leftIndicatorResId = a.getResourceId(R.styleable.ThreePaneLayout_leftActiveIndicator, 0);
        if (leftIndicatorResId != 0) {
            mLeftActiveIndicator = BitmapFactory.decodeResource(getResources(), leftIndicatorResId);
        }

        final int middleIndicatorResId = a.getResourceId(R.styleable.ThreePaneLayout_middleActiveIndicator, 0);
        if (middleIndicatorResId != 0) {
            mMiddleActiveIndicator = BitmapFactory.decodeResource(getResources(), middleIndicatorResId);
        }

        a.recycle();

        mShadow = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, new int[] {
                0xFF000000,
                0x00000000,
        });
        mDropShadowWidth = (int) (getResources().getDisplayMetrics().density * DEFAULT_DROP_SHADOW_WIDTH_DP + 0.5f);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnScrollChangedListener(mScrollChangedListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnScrollChangedListener(mScrollChangedListener);
        super.onDetachedFromWindow();
    }

    private int dpToPx(int dp) {
        return (int) (getResources().getDisplayMetrics().density * dp + 0.5f);
    }

    public void setLeftPaneLayout(int layoutId) {
        mLeftPane.removeAllViews();
        LayoutInflater.from(getContext()).inflate(layoutId, mLeftPane);
    }

    public void setMiddlePaneLayout(int layoutId) {
        mMiddlePane.removeAllViews();
        LayoutInflater.from(getContext()).inflate(layoutId, mMiddlePane);
    }

    public void setRightPaneLayout(int layoutId) {
        mRightPane.removeAllViews();
        LayoutInflater.from(getContext()).inflate(layoutId, mRightPane);
    }

    public void showLeftPane() {
        showLeftPane(true);
    }

    public void showLeftPane(boolean animate) {
        mVisiblePanes = PANE_LEFT | PANE_MIDDLE;
        requestLayout();
        animateOffsetTo(0.0f, animate);
        if (animate) setPageState(STATE_ANIMATE_LEFT);
    }

    public void showRightPane() {
        showRightPane(true);
    }

    public void showRightPane(boolean animate) {
        mVisiblePanes = PANE_RIGHT;
        if (mMiddlePaneCollapsible) mVisiblePanes |= PANE_MIDDLE;
        requestLayout();
        animateOffsetTo(1.0f, animate);
        if (animate) setPageState(STATE_ANIMATE_RIGHT);
    }

    public boolean isLeftPaneVisible() {
        return (mVisiblePanes & PANE_LEFT) != 0;
    }

    public boolean isMiddlePaneShowing() {
        return (mVisiblePanes & PANE_MIDDLE) != 0;
    }

    public boolean isRightPaneVisible() {
        return (mVisiblePanes & PANE_RIGHT) != 0;
    }

    public boolean isMiddlePaneCollapsible() {
        return mMiddlePaneCollapsible;
    }

    public void setLeftActiveView(View v) {
        setLeftActiveView(v, 0);
    }

    public void setLeftActiveView(View v, int position) {
        setLeftActiveView(v, position, true);
    }

    public void setLeftActiveView(View v, int position, boolean animate) {
        final View oldView = mLeftActiveView;
        mLeftActiveView = v;
        mLeftActivePosition = position;

        if (oldView != null && animate) {
            startAnimatingLeftIndicator();
        } else {
            mLeftIndicatorOffset = 1.0f;
        }

        invalidate();
    }

    private void startAnimatingLeftIndicator() {
        mLeftIndicatorStartPos = mLeftIndicatorTop;
        mLeftIndicatorAnimating = true;
        mLeftIndicatorScroller.startScroll(0.0f, 1.0f, INDICATOR_ANIM_DURATION);

        animateLeftIndicatorInvalidate();
    }

    /**
     * Callback when each frame in the indicator animation should be drawn.
     */
    private void animateLeftIndicatorInvalidate() {
        if (mLeftIndicatorScroller.computeScrollOffset()) {
            mLeftIndicatorOffset = mLeftIndicatorScroller.getCurr();
            invalidate();

            if (!mLeftIndicatorScroller.isFinished()) {
                postOnAnimation(mLeftIndicatorRunnable);
                return;
            }
        }

        completeAnimatingLeftIndicator();
    }

    /**
     * Called when the indicator animation has completed.
     */
    private void completeAnimatingLeftIndicator() {
        mLeftIndicatorOffset = 1.0f;
        mLeftIndicatorAnimating = false;
        invalidate();
    }

    public void setMiddleActiveView(View v) {
        setMiddleActiveView(v, 0);
    }

    public void setMiddleActiveView(View v, int position) {
        setMiddleActiveView(v, position, true);
    }

    public void setMiddleActiveView(View v, int position, boolean animate) {
        final View oldView = mMiddleActiveView;
        mMiddleActiveView = v;
        mMiddleActivePosition = position;

        if (oldView != null && animate) {
            startAnimatingMiddleIndicator();
        } else {
            mMiddleIndicatorOffset = 1.0f;
        }

        invalidate();
    }

    private void startAnimatingMiddleIndicator() {
        mMiddleIndicatorStartPos = mMiddleIndicatorTop;
        mMiddleIndicatorAnimating = true;
        mMiddleIndicatorScroller.startScroll(0.0f, 1.0f, INDICATOR_ANIM_DURATION);

        animateMiddleIndicatorInvalidate();
    }

    /**
     * Callback when each frame in the indicator animation should be drawn.
     */
    private void animateMiddleIndicatorInvalidate() {
        if (mMiddleIndicatorScroller.computeScrollOffset()) {
            mMiddleIndicatorOffset = mMiddleIndicatorScroller.getCurr();
            invalidate();

            if (!mMiddleIndicatorScroller.isFinished()) {
                postOnAnimation(mMiddleIndicatorRunnable);
                return;
            }
        }

        completeAnimatingMiddleIndicator();
    }

    /**
     * Called when the indicator animation has completed.
     */
    private void completeAnimatingMiddleIndicator() {
        mMiddleIndicatorOffset = 1.0f;
        mMiddleIndicatorAnimating = false;
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        final int height = getHeight();
        final int dropShadowWidth = mDropShadowWidth;

        final int middlePaneLeft = (int) (mMiddlePane.getLeft() + Math.floor(mMiddlePane.getTranslationX()));
        mShadow.setBounds(middlePaneLeft - dropShadowWidth, 0, middlePaneLeft, height);
        mShadow.draw(canvas);

        final int rightPaneLeft = (int) (mRightPane.getLeft() + Math.floor(mRightPane.getTranslationX()));
        final int scaledDropShadowWidth = (int) (dropShadowWidth * SMOOTH_INTERPOLATOR.getInterpolation(mOffset));
        mShadow.setBounds(rightPaneLeft - scaledDropShadowWidth, 0, rightPaneLeft, height);
        mShadow.draw(canvas);

        drawLeftIndicator(canvas);
        drawMiddleIndicator(canvas);
    }

    private void drawLeftIndicator(Canvas canvas) {
        if (mLeftActiveView != null && isViewDescendant(mLeftActiveView)) {
            Integer position = (Integer) mLeftActiveView.getTag(R.id.tplActiveViewPosition);
            final int pos = position == null ? 0 : position;

            if (pos == mLeftActivePosition) {

                mLeftActiveView.getDrawingRect(mLeftActiveRect);
                offsetDescendantRectToMyCoords(mLeftActiveView, mLeftActiveRect);

                if (mLeftIndicatorAnimating) {
                    final int indicatorFinalTop = mLeftActiveRect.top + ((mLeftActiveRect.height()
                            - mLeftActiveIndicator.getHeight()) / 2);
                    final int indicatorStartTop = mLeftIndicatorStartPos;
                    final int diff = indicatorFinalTop - indicatorStartTop;
                    final int startOffset = (int) (diff * mLeftIndicatorOffset);
                    mLeftIndicatorTop = indicatorStartTop + startOffset;
                } else {
                    mLeftIndicatorTop = mLeftActiveRect.top + ((mLeftActiveRect.height()
                            - mLeftActiveIndicator.getHeight()) / 2);
                }
                final int right = (int) (mMiddlePane.getLeft() + Math.floor(mMiddlePane.getTranslationX()));
                final int left = right - mLeftActiveIndicator.getWidth();

                canvas.save();
                canvas.clipRect(left, 0, right, getHeight());
                canvas.drawBitmap(mLeftActiveIndicator, left, mLeftIndicatorTop, null);
                canvas.restore();
            }
        }
    }

    private void drawMiddleIndicator(Canvas canvas) {
        if (mMiddleActiveView != null && isViewDescendant(mMiddleActiveView)) {
            Integer position = (Integer) mMiddleActiveView.getTag(R.id.tplActiveViewPosition);
            final int pos = position == null ? 0 : position;

            if (pos == mMiddleActivePosition) {

                mMiddleActiveView.getDrawingRect(mMiddleActiveRect);
                offsetDescendantRectToMyCoords(mMiddleActiveView, mMiddleActiveRect);

                final float interpolatedRatio = 1.f - INDICATOR_INTERPOLATOR.getInterpolation(1.0f - mOffset);
                final int interpolatedWidth = (int) (mMiddleActiveIndicator.getWidth() * interpolatedRatio);

                if (mMiddleIndicatorAnimating) {
                    final int indicatorFinalTop = mMiddleActiveRect.top + ((mMiddleActiveRect.height()
                            - mMiddleActiveIndicator.getHeight()) / 2);
                    final int indicatorStartTop = mMiddleIndicatorStartPos;
                    final int diff = indicatorFinalTop - indicatorStartTop;
                    final int startOffset = (int) (diff * mMiddleIndicatorOffset);
                    mMiddleIndicatorTop = indicatorStartTop + startOffset;
                } else {
                    mMiddleIndicatorTop = mMiddleActiveRect.top + ((mMiddleActiveRect.height()
                            - mMiddleActiveIndicator.getHeight()) / 2);
                }
                final int right = (int) (mRightPane.getLeft() + mRightPane.getTranslationX());
                final int left = right - interpolatedWidth;

                canvas.save();
                canvas.clipRect(left, 0, right, getHeight());
                canvas.drawBitmap(mMiddleActiveIndicator, left, mMiddleIndicatorTop, null);
                canvas.restore();
            }
        }
    }

    protected boolean isViewDescendant(View v) {
        ViewParent parent = v.getParent();
        while (parent != null) {
            if (parent == this) {
                return true;
            }

            parent = parent.getParent();
        }

        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = r - l;
        final int height = b - t;
        final float offset = mOffset;

        // This is easy. Only support 3.2+, so all moving or views are done with translations
        final int leftPaneWidth = mLeftPaneWidth;
        mLeftPane.layout(0, 0, leftPaneWidth, height);

        final int middlePaneWidth = mMiddlePane.getMeasuredWidth();
        mMiddlePane.layout(leftPaneWidth, 0, leftPaneWidth + middlePaneWidth, height);

        final int rightPaneWidth = mRightPane.getMeasuredWidth();
        mRightPane.layout(width, 0, width + rightPaneWidth, height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Must measure with an exact size");
        }

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(width, height);

        // Measure left pane
        final int leftPaneWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, mLeftPaneWidth);
        final int leftPaneHeightMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, height);
        mLeftPane.measure(leftPaneWidthMeasureSpec, leftPaneHeightMeasureSpec);

        // Measure middle pane
        final int middlePaneHeightMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, height);
        int middlePaneWidthMeasureSpec;

        mMiddlePaneExpandedWidth = width - mLeftPaneWidth;

        if (isRightPaneVisible() && mMiddlePaneCollapsible) {
            middlePaneWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, mMiddlePaneCollapsedWidth);
        } else {
            middlePaneWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, mMiddlePaneExpandedWidth);
        }

        mMiddlePane.measure(middlePaneWidthMeasureSpec, middlePaneHeightMeasureSpec);

        // Measure right pane
        int rightWidthMeasureSpec;
        final int rightHeightMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, height);

        if (mMiddlePaneCollapsible) {
            rightWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, width - mMiddlePaneCollapsedWidth);
        } else {
            rightWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, width);
        }

        final int rightWidth = MeasureSpec.getSize(rightWidthMeasureSpec);

        mRightPane.measure(rightWidthMeasureSpec, rightHeightMeasureSpec);

        // Just making sure it updates the translations
        setOffset(mOffset);
    }

    private void setOffset(float offset) {
        mOffset = offset;

        invalidate();

        final int width = getWidth();
        if (mMiddlePaneCollapsible) {
            final int middlePaneOffset = (int) (-mLeftPaneWidth * offset);
            mLeftPane.setTranslationX(middlePaneOffset);
            mMiddlePane.setTranslationX(middlePaneOffset);
            final int rightPaneTranslation = (int) (-(width - mMiddlePaneCollapsedWidth) * offset);
            mRightPane.setTranslationX(rightPaneTranslation);
        } else {
            final int viewOffset = (int) (-width * offset);
            mLeftPane.setTranslationX(viewOffset);
            mMiddlePane.setTranslationX(viewOffset);
            mRightPane.setTranslationX(viewOffset);
        }
    }

    public void setPaneStateChangeListener(OnPaneStateChangeListener paneStateChangeListener) {
        mPaneStateChangeListener = paneStateChangeListener;
    }

    private void setPageState(int state) {
        if (state != mPageState) {
            if (mPaneStateChangeListener != null) mPaneStateChangeListener.onPaneStateChange(mPageState, state);
            mPageState = state;
        }
    }

    /**
     * If possible, set the layer type to {@link View#LAYER_TYPE_HARDWARE}.
     */
    protected void startLayerTranslation() {
        if (USE_TRANSLATIONS && !mLayerTypeHardware) {
            mLayerTypeHardware = true;
            mLeftPane.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mMiddlePane.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mRightPane.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }

    /**
     * If the current layer type is {@link View#LAYER_TYPE_HARDWARE}, this will set it to @link View#LAYER_TYPE_NONE}.
     */
    private void stopLayerTranslation() {
        if (mLayerTypeHardware) {
            mLayerTypeHardware = false;
            mLeftPane.setLayerType(View.LAYER_TYPE_NONE, null);
            mMiddlePane.setLayerType(View.LAYER_TYPE_NONE, null);
            mRightPane.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }

    protected void stopAnimation() {
        removeCallbacks(mDragRunnable);
        mScroller.abortAnimation();
        stopLayerTranslation();
    }

    private void completeAnimation() {
        mScroller.abortAnimation();
        final float finalVal = mScroller.getFinal();
        setOffset(finalVal);
        setPageState(isLeftPaneVisible() ? STATE_LEFT_VISIBLE : STATE_RIGHT_VISIBLE);
        stopLayerTranslation();
    }

    protected void animateOffsetTo(float finalOffset, boolean animate) {
        if (!animate) {
            setOffset(finalOffset);
            return;
        }

        final float start = mOffset;
        final float dx = finalOffset - start;

        int duration = (int) (DURATION_MAX * Math.abs(dx));
        mScroller.startScroll(start, dx, duration);

        startLayerTranslation();
        postAnimationInvalidate();
    }

    /**
     * Callback when each frame in the drawer animation should be drawn.
     */
    private void postAnimationInvalidate() {
        if (mScroller.computeScrollOffset()) {
            final float curr = mScroller.getCurr();

            setOffset(curr);
            if (!mScroller.isFinished()) {
                postOnAnimation(mDragRunnable);
                return;
            }
        }

        completeAnimation();
    }

    @Override
    public void postOnAnimation(Runnable action) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            super.postOnAnimation(action);
        } else {
            postDelayed(action, ANIMATION_DELAY);
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        if (savedState.mRightPaneVisible) showRightPane(false);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);

        state.mRightPaneVisible = isRightPaneVisible();

        return state;
    }

    static class SavedState extends BaseSavedState {

        boolean mRightPaneVisible;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel in) {
            super(in);
            mRightPaneVisible = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mRightPaneVisible ? 1 : 0);
        }

        @SuppressWarnings("UnusedDeclaration")
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
