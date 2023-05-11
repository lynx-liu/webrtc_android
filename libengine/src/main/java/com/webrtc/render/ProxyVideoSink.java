package com.webrtc.render;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.util.Log;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ProxyVideoSink implements VideoSink {
    private static final String TAG = "ProxyVideoSink";
    private VideoSink target;
    private final int outputWidth = 1280;
    private final int outputHeight = 720;
	private final int frameSize = outputWidth * outputHeight;
	private final int chromaSize = frameSize >> 2;
    private final byte[] yuvData = new byte[frameSize*3/2];

    private OutputStream outputStream = null;
    private static LocalSocket localSocket = null;
    private final LocalSocketAddress localSocketAddress = new LocalSocketAddress("/dev/socket/video0",LocalSocketAddress.Namespace.FILESYSTEM);

    public ProxyVideoSink() {
        if(localSocket==null) {
            localSocket = new LocalSocket(LocalSocket.SOCKET_STREAM);
        }
    }

    @Override
    synchronized public void onFrame(VideoFrame frame) {
        if (Build.DEVICE.equals("generic_x86_64") && frame.getBuffer() instanceof VideoFrame.I420Buffer) {
            long start = System.currentTimeMillis();
            VideoFrame.I420Buffer i420Buffer = (VideoFrame.I420Buffer) frame.getBuffer();
            int width = i420Buffer.getWidth();
            int height = i420Buffer.getHeight();
            Log.d(TAG,"width:"+width+", height:"+height);

            if(width!=outputWidth || height!=outputHeight) {
                float scaleFactor = Math.min((float) outputHeight / height, (float) outputWidth / width);
                int newWidth = (int) (scaleFactor * width);
                int newHeight = (int) (scaleFactor * height);
                VideoFrame.I420Buffer i420ScaleBuffer = (VideoFrame.I420Buffer) i420Buffer.cropAndScale(0, 0, width, height, newWidth, newHeight);
                injectCamera(i420ScaleBuffer);
                i420ScaleBuffer.release();
            } else {
                injectCamera(i420Buffer);
            }
            Log.d("llx","time:"+(System.currentTimeMillis()-start));
        }

        if (target == null) {
            Log.d(TAG, "Dropping frame in proxy because target is null.");
            return;
        }
        target.onFrame(frame);
    }

    private boolean injectCamera(VideoFrame.I420Buffer i420Buffer) {
        try {
            if(!localSocket.isConnected()) {
                Log.d(TAG,"Connect ...");
                localSocket.connect(localSocketAddress);
                localSocket.setSendBufferSize(yuvData.length);
            }

            if(localSocket.isConnected()) {
                if(outputStream==null) {
                    outputStream = localSocket.getOutputStream();
                }

                if(outputStream!=null) {
                    ByteBuffer yBuffer = i420Buffer.getDataY();
                    yBuffer.get(yuvData,0,yBuffer.remaining());
                    yBuffer.clear();

                    ByteBuffer uBuffer = i420Buffer.getDataU();
                    uBuffer.get(yuvData,frameSize,uBuffer.remaining());
                    uBuffer.clear();

                    ByteBuffer vBuffer = i420Buffer.getDataV();
                    vBuffer.get(yuvData,frameSize+chromaSize,vBuffer.remaining());
                    vBuffer.clear();

                    outputStream.write(yuvData);
                    return true;
                } else {
                    Log.d(TAG, "outputStream is null");
                }
            } else {
                Log.d(TAG,"Connect Fail");
            }
        } catch (Exception e) {
            Log.d(TAG,e.toString());
            try {
                if(outputStream!=null) {
                    outputStream.close();
                    outputStream = null;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    localSocket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            localSocket = new LocalSocket(LocalSocket.SOCKET_STREAM);
            Log.d(TAG,"new LocalSocket");
        }
        return false;
    }

    synchronized public void setTarget(VideoSink target) {
        this.target = target;
    }
}