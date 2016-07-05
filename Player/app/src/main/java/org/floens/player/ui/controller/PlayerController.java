package org.floens.player.ui.controller;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.floens.controller.utils.AndroidUtils;
import org.floens.controller.Controller;
import org.floens.controller.transition.FadeOutTransition;
import org.floens.controller.ui.drawable.ArrowMenuDrawable;
import org.floens.controller.utils.InsetsHelper;
import org.floens.player.PlayerApplication;
import org.floens.player.R;
import org.floens.mpv.egl.EGLView;
import org.floens.player.ui.layout.PlayerControllerContainer;
import org.floens.player.ui.layout.PlayerControls;
import org.floens.player.core.model.FileItem;
import org.floens.mpv.MpvCore;
import org.floens.mpv.renderer.MpvRenderer;

import static org.floens.controller.utils.AndroidUtils.setRoundItemBackground;

public class PlayerController extends Controller implements View.OnClickListener, PlayerControls.Callback, View.OnSystemUiVisibilityChangeListener, View.OnTouchListener, PlayerControllerContainer.Callback {
    private static final String TAG = "PlayerController";

    private static final long HIDE_TIME = 1200;

    private PlayerControllerContainer container;
    private EGLView playerSurface;
    private ImageView back;
    private ViewGroup controlsContainer;
    private PlayerControls playerControls;

    private Handler handler;
    private GestureDetector gestureDetector;

    private Runnable hideUiRunnable = new Runnable() {
        @Override
        public void run() {
            setUiHidden(true);
        }
    };

    private FileItem fileItem;
    private boolean playing = false;
    private boolean touchDown = false;

    private MpvCore mpvCore;
    private MpvRenderer mpvRenderer;

    public PlayerController(Context context) {
        super(context);
    }

    public void setFileItem(FileItem fileItem) {
        this.fileItem = fileItem;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        view = inflateRes(R.layout.controller_player);
        container = (PlayerControllerContainer) view;
        container.setCallback(this);

        controlsContainer = (ViewGroup) view.findViewById(R.id.controls_container);
        InsetsHelper.attachInsetsMargin(controlsContainer, true, true, true, true);

        playerSurface = (EGLView) view.findViewById(R.id.player_surface);

        back = (ImageView) view.findViewById(R.id.back);
        ArrowMenuDrawable drawable = new ArrowMenuDrawable();
        drawable.setProgress(1f);
        back.setOnClickListener(this);
        back.setImageDrawable(drawable);
        setRoundItemBackground(back);

        playerControls = (PlayerControls) view.findViewById(R.id.player_controls);
        playerControls.setCallback(this);

        view.setOnSystemUiVisibilityChangeListener(this);
        view.setOnTouchListener(this);
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return PlayerController.this.onSingleTapUp(e);
            }
        });

        handler = new Handler();

        initialize();
    }

    private void initialize() {
        mpvCore = PlayerApplication.getInstance().getMpvCore();
        mpvRenderer = new MpvRenderer(mpvCore);
        playerSurface.setRenderer(mpvRenderer);
    }

    @Override
    public void onShow() {
        super.onShow();
        view.requestApplyInsets();


        AndroidUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mpvCore.command(new String[]{
                        "loadfile", fileItem.file.getAbsolutePath()
                });
            }
        }, 500);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void prevButtonClicked() {
    }

    @Override
    public void nextButtonClicked() {
    }

    @Override
    public void pauseButtonClicked() {
        setPlaying(false);
    }

    @Override
    public void playButtonClicked() {
        setPlaying(true);
    }

    @Override
    public boolean onBack() {
        navigationController.popController(new FadeOutTransition());
        setPlaying(false);
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == back) {
            onBack();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        onTouch(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEventCalled(MotionEvent event) {
        onTouch(event);
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        rescheduleHide();
    }

    private void onTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDown = true;
                Log.i(TAG, "touchDown = true");
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchDown = false;
                Log.i(TAG, "touchDown = false");
                break;
        }
        rescheduleHide();
    }

    private boolean onSingleTapUp(MotionEvent e) {
        setUiHidden(!isNavigationHidden());

        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
//        Log.d(TAG, "onWindowFocusChanged() called with: " + "hasFocus = [" + hasFocus + "]");

        if (!hasFocus) {
            setPlaying(false);
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
//        Log.i(TAG, "onSystemUiVisibilityChange() called with: " + "visibility = [" + visibility + "]");
        rescheduleHide();
        playerControls.setHidden(isNavigationHidden(), true);
    }

    private void setPlaying(boolean playing) {
        this.playing = playing;
        playerControls.setPlaying(playing);
        rescheduleHide();
        if (isNavigationHidden() && !playing) {
            setUiHidden(false);
        }
    }

    private void rescheduleHide() {
        handler.removeCallbacks(hideUiRunnable);
        if (!isNavigationHidden() && playing && !touchDown) {
            Log.i(TAG, "scheduling hide");
            handler.postDelayed(hideUiRunnable, HIDE_TIME);
        }
    }

    private void setUiHidden(boolean uiHidden) {
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (uiHidden) {
            flags |= View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE;
        }

        view.setSystemUiVisibility(flags);
    }

    private boolean isNavigationHidden() {
        return (view.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0;
    }
}