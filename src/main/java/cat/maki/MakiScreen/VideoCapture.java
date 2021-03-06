package cat.maki.MakiScreen;

import org.bukkit.map.MapCanvas;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

// https://stackoverflow.com/questions/21420252/how-to-receive-mpeg-ts-stream-over-udp-from-ffmpeg-in-java
class VideoCaptureUDPServer extends Thread {
    public boolean running = true;

    private DatagramSocket socket;

    public void onFrame(BufferedImage frame) { }

    public void run() {
        try {
            byte[] buffer = new byte[1024*1024]; // 1 mb
            socket = new DatagramSocket(1337);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            while (running) {
                socket.receive(packet);

                byte[] data = packet.getData();

                if (data[0]==-1 && data[1]==-40) { // FF D8 (start of file)
                    if (output.size()>0) {
                        try {
                            ByteArrayInputStream stream = new ByteArrayInputStream(output.toByteArray());
                            onFrame(ImageIO.read(stream));
                        } catch (IOException e) {}

                        output.reset();
                    }
                }

                output.write(data,0,packet.getLength());
                //System.out.println(String.format("%02X", data[0])+" "+String.format("%02X", data[1]));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {
        running = false;
        if (socket!=null) socket.disconnect();
        if (socket!=null) socket.close();
    }
}

public class VideoCapture extends Thread {
    public Boolean active = true;
    public int width;
    public int height;

    private BufferedImage currentFrame;

    VideoCaptureUDPServer videoCaptureUDPServer;

    private ProcessBuilder ffmpegCommand;


    public void renderCanvas(int id, MapCanvas mapCanvas) {
        BufferedImage frame = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = frame.createGraphics();
        switch (id) {
            case 0: graphics.drawImage(currentFrame,0,0,null); break;
            case 1: graphics.drawImage(currentFrame,-128,0,null); break;
            //case 2: graphics.drawImage(currentFrame,-256,0,null); break;
            //case 3: graphics.drawImage(currentFrame,0,-128,null); break;
            //case 4: graphics.drawImage(currentFrame,-128,-128,null); break;
            //case 5: graphics.drawImage(currentFrame,-256,-128,null); break;
        }

        mapCanvas.drawImage(0,0, frame);
        graphics.dispose();
    }

    public VideoCapture(int width, int height) {
        this.width = width;
        this.height = height;

        currentFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        videoCaptureUDPServer = new VideoCaptureUDPServer() {
            @Override
            public void onFrame(BufferedImage frame) {
                currentFrame = frame;
            }
        };

        videoCaptureUDPServer.start();

//        command = new ProcessBuilder(
//            ("ffmpeg -y -f dshow -i video=\"OBS-Camera\" -vf scale="+width+":"+height+" -f rawvideo -c:v mjpeg -qscale:v 1 -r 20 tcp://127.0.0.1:1337")
//                .split(" ")
//        );
    }

    public void run() {
//        while (active) {
//            onFrame(getFrame());
//        }
    }

    public void cleanup() {
        videoCaptureUDPServer.cleanup();
    }

    public static void main(String[] args) {
        //new VideoCapture(128*3, 128*2).start();
    }
}
