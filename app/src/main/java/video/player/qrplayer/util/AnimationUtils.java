package video.player.qrplayer.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import video.player.qrplayer.activity.HomeActivity;

public class AnimationUtils {
    private static final String TAG = "AnimationUtils";
    private static final boolean DEBUG = HomeActivity.DEBUG;

    public static void animateView(View view, boolean enterOrExit, long duration) {
        animateView(view, Type.ALPHA, enterOrExit, duration, 0, null);
    }

    public static void animateView(View view, boolean enterOrExit, long duration, long delay) {
        animateView(view, Type.ALPHA, enterOrExit, duration, delay, null);
    }

    public static void animateView(View view, boolean enterOrExit, long duration, long delay, Runnable execOnEnd) {
        animateView(view, Type.ALPHA, enterOrExit, duration, delay, execOnEnd);
    }

    public static void animateView(View view, Type animationType, boolean enterOrExit, long duration) {
        animateView(view, animationType, enterOrExit, duration, 0, null);
    }

    public static void animateView(View view, Type animationType, boolean enterOrExit, long duration, long delay) {
        animateView(view, animationType, enterOrExit, duration, delay, null);
    }

    public static void animateView(final View view, Type animationType, boolean enterOrExit, long duration, long delay, Runnable execOnEnd) {
        if (DEBUG) {
            String id;
            try {
                id = view.getResources().getResourceEntryName(view.getId());
            } catch (Exception e) {
                id = view.getId() + "";
            }

            String msg = String.format("%8s →  [%s:%s] [%s %s:%s] execOnEnd=%s",
                    enterOrExit, view.getClass().getSimpleName(), id, animationType, duration, delay, execOnEnd);
            Log.d(TAG, "animateView()" + msg);
        }

        if (view.getVisibility() == View.VISIBLE && enterOrExit) {
            if (DEBUG) Log.d(TAG, "animateView() view was already visible > view = [" + view + "]");
            view.animate().setListener(null).cancel();
            view.setVisibility(View.VISIBLE);
            view.setAlpha(1f);
            if (execOnEnd != null) execOnEnd.run();
            return;
        } else if ((view.getVisibility() == View.GONE || view.getVisibility() == View.INVISIBLE) && !enterOrExit) {
            if (DEBUG) Log.d(TAG, "animateView() view was already gone > view = [" + view + "]");
            view.animate().setListener(null).cancel();
            view.setVisibility(View.GONE);
            view.setAlpha(0f);
            if (execOnEnd != null) execOnEnd.run();
            return;
        }

        view.animate().setListener(null).cancel();
        view.setVisibility(View.VISIBLE);

        switch (animationType) {
            case ALPHA:
                animateAlpha(view, enterOrExit, duration, delay, execOnEnd);
                break;
            case SCALE_AND_ALPHA:
                animateScaleAndAlpha(view, enterOrExit, duration, delay, execOnEnd);
                break;
            case LIGHT_SCALE_AND_ALPHA:
                animateLightScaleAndAlpha(view, enterOrExit, duration, delay, execOnEnd);
                break;
            case SLIDE_AND_ALPHA:
                animateSlideAndAlpha(view, enterOrExit, duration, delay, execOnEnd);
                break;
            case LIGHT_SLIDE_AND_ALPHA:
                animateLightSlideAndAlpha(view, enterOrExit, duration, delay, execOnEnd);
                break;
        }
    }

    public static ValueAnimator animateHeight(final View view, long duration, int targetHeight) {
        final int height = view.getHeight();
        if (DEBUG) {
            Log.d(TAG, "animateHeight: duration = [" + duration + "], from " + height + " to → " + targetHeight + " in: " + view);
        }

        ValueAnimator animator = ValueAnimator.ofFloat(height, targetHeight);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.setDuration(duration);
        animator.addUpdateListener(animation -> {
            final float value = (float) animation.getAnimatedValue();
            view.getLayoutParams().height = (int) value;
            view.requestLayout();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.getLayoutParams().height = targetHeight;
                view.requestLayout();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                view.getLayoutParams().height = targetHeight;
                view.requestLayout();
            }
        });
        animator.start();

        return animator;
    }

    public static void animateRotation(final View view, long duration, int targetRotation) {
        if (DEBUG) {
            Log.d(TAG, "animateRotation: duration = [" + duration + "], from " + view.getRotation() + " to → " + targetRotation + " in: " + view);
        }
        view.animate().setListener(null).cancel();

        view.animate().rotation(targetRotation).setDuration(duration).setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        view.setRotation(targetRotation);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setRotation(targetRotation);
                    }
                }).start();
    }

    private static void animateAlpha(final View view, boolean enterOrExit, long duration, long delay, final Runnable execOnEnd) {
        if (enterOrExit) {
            view.animate().setInterpolator(new FastOutSlowInInterpolator()).alpha(1f)
                    .setDuration(duration).setStartDelay(delay).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (execOnEnd != null) execOnEnd.run();
                }
            }).start();
        } else {
            view.animate().setInterpolator(new FastOutSlowInInterpolator()).alpha(0f)
                    .setDuration(duration).setStartDelay(delay).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                    if (execOnEnd != null) execOnEnd.run();
                }
            }).start();
        }
    }

    private static void animateScaleAndAlpha(final View view, boolean enterOrExit, long duration, long delay, final Runnable execOnEnd) {
        if (enterOrExit) {
            view.setScaleX(.8f);
            view.setScaleY(.8f);
            view.animate().setInterpolator(new FastOutSlowInInterpolator()).alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(duration).setStartDelay(delay).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (execOnEnd != null) execOnEnd.run();
                }
            }).start();
        } else {
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.animate().setInterpolator(new FastOutSlowInInterpolator()).alpha(0f).scaleX(.8f).scaleY(.8f)
                    .setDuration(duration).setStartDelay(delay).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                    if (execOnEnd != null) execOnEnd.run();
                }
            }).start();
        }
    }

    private static void animateLightScaleAndAlpha(final View view, boolean enterOrExit, long duration, long delay, final Runnable execOnEnd) {
        if (enterOrExit) {
            view.setAlpha(.5f);
            view.setScaleX(.95f);
            view.setScaleY(.95f);
            view.animate().setInterpolator(new FastOutSlowInInterpolator()).alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(duration).setStartDelay(delay).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (execOnEnd != null) execOnEnd.run();
                }
            }).start();
        } else {
            view.setAlpha(1f);
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.animate().setInterpolator(new FastOutSlowInInterpolator()).alpha(0f).scaleX(.95f).scaleY(.95f)
                    .setDuration(duration).setStartDelay(delay).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                    if (execOnEnd != null) execOnEnd.run();
                }
            }).start();
        }
    }

    private static void animateSlideAndAlpha(final View view, boolean enterOrExit, long duration, long delay, final Runnable execOnEnd) {
        if (enterOrExit) {
            view.setTranslationY(-view.getHeight());
            view.setAlpha(0f);
            view.animate().setInterpolator(new FastOutSlowInInterpolator()).alpha(1f).translationY(0)
                    .setDuration(duration).setStartDelay(delay).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (execOnEnd != null) execOnEnd.run();
                }
            }).start();
        } else {
            view.animate().setInterpolator(new FastOutSlowInInterpolator()).alpha(0f).translationY(-view.getHeight())
                    .setDuration(duration).setStartDelay(delay).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                    if (execOnEnd != null) execOnEnd.run();
                }
            }).start();
        }
    }

    private static void animateLightSlideAndAlpha(final View view, boolean enterOrExit, long duration, long delay, final Runnable execOnEnd) {
        if (enterOrExit) {
            view.setTranslationY(-view.getHeight() / 2);
            view.setAlpha(0f);
            view.animate().setInterpolator(new FastOutSlowInInterpolator()).alpha(1f).translationY(0)
                    .setDuration(duration).setStartDelay(delay).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (execOnEnd != null) execOnEnd.run();
                }
            }).start();
        } else {
            view.animate().setInterpolator(new FastOutSlowInInterpolator()).alpha(0f).translationY(-view.getHeight() / 2)
                    .setDuration(duration).setStartDelay(delay).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                    if (execOnEnd != null) execOnEnd.run();
                }
            }).start();
        }
    }

    public enum Type {
        ALPHA,
        SCALE_AND_ALPHA, LIGHT_SCALE_AND_ALPHA,
        SLIDE_AND_ALPHA, LIGHT_SLIDE_AND_ALPHA
    }
}
