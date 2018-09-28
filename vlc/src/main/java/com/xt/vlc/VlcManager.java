package com.xt.vlc;

import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

/**
 * Created by Administrator on 2017/4/25.
 */

public class VlcManager {
    private static VlcManager instance = null;
    private Point point;
    private int displayWidth;
    private int videoWidth;
    private int videoHight;

    private VlcManager(Context context) {
        initVlc();
        initDiaplayWidth(context);
    }

    private void initDiaplayWidth(Context context) {
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        point = new Point();
        display.getSize(point);
        setDisplayWidth(false);
    }

    /**
     * 因为是横屏，所以获取的屏幕的宽高要对调
     *
     * @param isFullScreen 是否全屏，全屏时屏幕宽度等于获取的屏幕高度，半屏时屏幕宽度等于获取的屏幕高度的一半
     */
    public void setDisplayWidth(boolean isFullScreen) {
        if (isFullScreen) {
            displayWidth = point.x;
//            XTLogUtil.d("x--------->" + point.x);
//            XTLogUtil.d("y--------->" + point.y);
//            displayHeight = point.y;
        } else {
            displayWidth = point.x / 2;
//            displayHeight = point.y/2;
        }
    }

    public static VlcManager getInstance(Context context) {
        if (instance == null) {
            instance = new VlcManager(context);
        }
        return instance;
    }

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private IVLCVout vlcVout;
    private IVLCVout.Callback callback;
    private MediaPlayer mediaPlayer;
    private MediaPlayer.EventListener eventListener;
    private LibVLC libvlc;

    public void addEventListener(MediaPlayer.EventListener eventListener) {
        mediaPlayer.setEventListener(eventListener);
        this.eventListener = eventListener;
    }

    private void initVlc() {
        libvlc = LibVLCUtil.getLibVLC(null);
        mediaPlayer = new MediaPlayer(libvlc);
        String MRL_RTSP = "rtsp://192.168.1.1/live1.sdp";
        Media media = new Media(libvlc, Uri.parse(MRL_RTSP));
        media.setHWDecoderEnabled(true, true);
        media.addOption(":network-caching=250");
        /*media.addOption(":live-caching=250");
        media.addOption(":file-caching=250");
        media.addOption(":codec=mediacodec,iomx,all");*/
        mediaPlayer.setMedia(media);
//        eventListener = new VlcEventListener();
//        mediaPlayer.setEventListener(eventListener);
        vlcVout = mediaPlayer.getVLCVout();
        callback = new VlcCallback();
        vlcVout.addCallback(callback);
    }

    class VlcCallback implements IVLCVout.Callback {

        @Override
        public void onNewLayout(IVLCVout ivlcVout, int i, int i1, int i2, int i3, int i4, int i5) {
//            changeSurfaceViewSize(i, i1);
            if (videoWidth == i && videoHight == i1) {
                return;
            } else {
                videoWidth = i;
                videoHight = i1;
                changeSurfaceViewSize();
            }
        }

        @Override
        public void onSurfacesCreated(IVLCVout ivlcVout) {
//            XTLogUtil.d("onSurfacesCreated-----------");
        }

        @Override
        public void onSurfacesDestroyed(IVLCVout ivlcVout) {
//            XTLogUtil.d("onSurfacesDestroyed--------------");
        }
    }

    public void changeSurfaceViewSize() {
        changeSurfaceViewSize(videoWidth, videoHight);
    }

    //总结：surfaceview的宽高比要等于视频的宽高比
    private void changeSurfaceViewSize(int i, int i1) {
        try {
            videoWidth = i;
            videoHight = i1;
            // calculate aspect ratio，视频的宽高比
            double ar = (double) videoWidth / (double) videoHight;
            // calculate display aspect ratio，视频显示区域的宽高比
            double dar = (double) displayWidth / point.y;
            surfaceHolder.setFixedSize(videoWidth, videoWidth);
            ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
            if (dar > ar) {
                layoutParams.height = point.y;
                layoutParams.width = (int) (layoutParams.height * ar);
            } else {
                layoutParams.width = displayWidth;
                layoutParams.height = (int) (layoutParams.width / ar);
            }
            surfaceView.setLayoutParams(layoutParams);
            surfaceView.invalidate();
        } catch (Exception e) {
            Log.d("vlc-newlayout", e.toString());
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void play() {
//        XTLogUtil.d("播放");
        mediaPlayer.play();
    }

    public void stopPlay() {
//        XTLogUtil.d("stopPlay");
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    public void onDestroy() {
        mediaPlayer.stop();
        mediaPlayer.setEventListener(null);
        vlcVout.removeCallback(callback);
        vlcVout.detachViews();
        mediaPlayer.release();
        libvlc.release();
        libvlc = null;
        surfaceHolder = null;
        surfaceView = null;
        instance = null;
    }

    public void attachView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        vlcVout.setVideoView(surfaceView);
        vlcVout.attachViews();
    }

    public void detachView() {
        stopPlay();
        vlcVout.detachViews();
    }
}
