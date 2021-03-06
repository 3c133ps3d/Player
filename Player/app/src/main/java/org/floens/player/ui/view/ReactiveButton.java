package org.floens.player.ui.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.View;

import org.floens.controller.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

public class ReactiveButton extends View implements View.OnClickListener {
    private List<Drawable> drawables = new ArrayList<>();
    private int selected = 0;

    private Drawable drawable;
    private Rect bounds = new Rect();
    private float drawableAlpha = 1f;
    private float drawableScale = 1f;
    private boolean doScaleAnimation = true;

    private Callback callback;

    public ReactiveButton(Context context) {
        this(context, null);
    }

    public ReactiveButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReactiveButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        AndroidUtils.setRoundItemBackground(this);

        setOnClickListener(this);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setDoScaleAnimation(boolean doScaleAnimation) {
        this.doScaleAnimation = doScaleAnimation;
    }

    public void addDrawable(Drawable drawable) {
        drawable = DrawableCompat.wrap(drawable.mutate());
        drawables.add(drawable);
        if (drawables.size() == 1) {
            this.drawable = drawables.get(0);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        invalidate();
    }

    public void setSelected(int selected, boolean animate) {
        if (this.selected != selected) {
            this.selected = selected;
            notifyButtonClicked(selected);

            if (animate) {
                final long duration = 160;

                ValueAnimator alphaAnimator = ObjectAnimator.ofFloat(1f, 0f, 1f);
                alphaAnimator.setDuration(duration);
                alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    private boolean switched;

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        drawableAlpha = (float) animation.getAnimatedValue();
                        if (animation.getAnimatedFraction() >= 0.5f && !switched) {
                            switched = true;
                            drawable = drawables.get(ReactiveButton.this.selected);
                        }
                        invalidate();
                    }
                });
                AnimatorSet set = new AnimatorSet();
                List<Animator> animators = new ArrayList<>(2);
                animators.add(alphaAnimator);
                if (doScaleAnimation) {
                    animators.add(getScaleAnimation());
                }
                set.playTogether(animators);
                set.start();
            } else {
                drawable = drawables.get(selected);
                invalidate();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (drawables.size() > 1) {
            setSelected((selected + 1) % drawables.size(), true);
        } else {
            if (doScaleAnimation) {
                getScaleAnimation().start();
            }
            notifyButtonClicked(0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int left = (getWidth() - drawable.getIntrinsicWidth()) / 2;
        int top = (getHeight() - drawable.getIntrinsicHeight()) / 2;

        bounds.set(left, top, left + drawable.getIntrinsicWidth(), top + drawable.getIntrinsicHeight());

        canvas.save();
        canvas.scale(drawableScale, drawableScale, getWidth() / 2f, getHeight() / 2f);

        drawable.setBounds(bounds);
        drawable.setAlpha((int) (255 * drawableAlpha * (isEnabled() ? 1f : 0.54f)));
        drawable.draw(canvas);
        canvas.restore();
    }

    private void notifyButtonClicked(int selected) {
        if (callback != null) {
            callback.onButtonClicked(this, selected);
        }
    }

    private ValueAnimator getScaleAnimation() {
        final float scale = 0.75f;
        final long duration = 160;

        ValueAnimator scaleAnimator = ObjectAnimator.ofFloat(1f, scale, 1f);
        scaleAnimator.setDuration(duration);
        scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                drawableScale = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        return scaleAnimator;
    }

    public interface Callback {
        void onButtonClicked(ReactiveButton button, int selected);
    }
}
