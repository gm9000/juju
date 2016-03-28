package com.juju.app.media.consumer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;


public class OutputStreamMediaConsumer extends MediaConsumer {

    private ArrayBlockingQueue<byte[]> mediaQueue;
    private OutputStream outputStream;

    public OutputStreamMediaConsumer(ArrayBlockingQueue<byte[]> mediaDataQueue, OutputStream consumer) {

        mediaQueue = mediaDataQueue;
        outputStream = consumer;
    }


    public boolean isConsuming = false;

    public void StopConsume() {
        isConsuming = false;
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void StartMediaConsumeThread() {
        Thread consumerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                isConsuming = true;
                byte[] mediaData = null;
                while (isConsuming) {
                    if (mediaQueue.size() > 0) {
                        mediaData = mediaQueue.poll();
                        if (mediaData != null) {
                            try {
                                outputStream.write(mediaData, 0, mediaData.length);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        consumerThread.start();
    }

}
