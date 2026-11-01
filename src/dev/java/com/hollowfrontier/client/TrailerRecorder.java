package com.hollowfrontier.client;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.hollowfrontier.HollowFrontier;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.GpuTexture;

/**
 * Dev-only (for {@link Trailer}): records the main render target to an mp4 through ffmpeg. Each frame is copied off the
 * GPU as vanilla's screenshot does, but read out in one bulk copy and handed to a writer thread, which feeds ffmpeg a
 * steady {@code fps}: at each output time it writes the newest frame rendered by then (repeating or skipping frames as
 * the game's own frame rate wobbles), so a clip runs at real speed whatever the game managed.
 */
final class TrailerRecorder {
    private static final String FFMPEG = System.getProperty("hollowfrontier.trailer.ffmpeg", "ffmpeg");
    /** h264_amf (the dev machine's Radeon), h264_nvenc, or libx264 on the CPU. */
    private static final String ENCODER = System.getProperty("hollowfrontier.trailer.encoder", "h264_amf");

    private record Frame(long nanos, byte[] rgba) {}

    private final int fps;
    private final long period;
    private final BlockingQueue<Frame> queue = new ArrayBlockingQueue<>(48);
    private int width;
    private int height;
    private Process process;
    private Thread writer;
    private volatile boolean open;
    private volatile long stopNanos = Long.MAX_VALUE;
    private int dropped;
    private int written;

    TrailerRecorder(int fps) {
        this.fps = fps;
        this.period = 1_000_000_000L / fps;
    }

    boolean recording() {
        return open;
    }

    /** Starts ffmpeg writing {@code file} from frames of the given size (the render target's, flipped and scaled to 1080p). */
    boolean start(Path file, int width, int height) {
        this.width = width;
        this.height = height;
        this.dropped = 0;
        this.written = 0;
        this.stopNanos = Long.MAX_VALUE;
        queue.clear();
        List<String> command = new ArrayList<>(List.of(FFMPEG, "-hide_banner", "-loglevel", "error", "-y",
                "-f", "rawvideo", "-pix_fmt", "rgba", "-s", width + "x" + height, "-r", Integer.toString(fps), "-i", "-",
                "-vf", "vflip,scale=1920:1080:flags=lanczos,format=yuv420p", "-c:v", ENCODER));
        command.addAll(switch (ENCODER) {
            case "h264_amf" -> List.of("-quality", "quality", "-rc", "cqp", "-qp_i", "16", "-qp_p", "18");
            case "h264_nvenc" -> List.of("-preset", "p6", "-cq", "17");
            default -> List.of("-preset", "veryfast", "-crf", "15");
        });
        command.add(file.toString());
        try {
            Files.createDirectories(file.getParent());
            process = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(file.resolveSibling(file.getFileName() + ".log").toFile()).start();
        } catch (IOException e) {
            HollowFrontier.LOGGER.error("[Trailer] can't start ffmpeg ({}): {}", FFMPEG, e.getMessage());
            return false;
        }
        OutputStream pipe = process.getOutputStream();
        writer = new Thread(() -> write(pipe), "Trailer recorder");
        writer.setDaemon(true);
        open = true;
        writer.start();
        return true;
    }

    /** On the render thread, once the frame is drawn: queues a copy of it, stamped with when it was rendered. */
    void capture(RenderTarget target, long nanos) {
        GpuTexture texture = target.getColorTexture();
        if (!open || texture == null || target.width != width || target.height != height || texture.getFormat().blockSize() != 4) {
            return;
        }
        int w = width, h = height;
        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Trailer frame", 9, (long) w * h * 4);
        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(texture, buffer, 0L, () -> {
            if (open) {
                try (GpuBufferSlice.MappedView read = buffer.map(true, false)) {
                    byte[] rgba = new byte[w * h * 4];
                    read.data().get(0, rgba);
                    if (!queue.offer(new Frame(nanos, rgba))) {
                        dropped++;
                    }
                }
            }
            buffer.close();
        }, 0);
    }

    /** Ends the clip at the given time (later frames still on the GPU are let go) and waits for ffmpeg to finish it. */
    void stop(long nanos) {
        if (!open) {
            return;
        }
        stopNanos = nanos;
        queue.offer(new Frame(nanos, null));
        try {
            writer.join(30_000);
            open = false;
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                HollowFrontier.LOGGER.warn("[Trailer] ffmpeg is still writing; leaving it to finish");
            } else if (process.exitValue() != 0) {
                HollowFrontier.LOGGER.warn("[Trailer] ffmpeg exited with {} (see the clip's .log)", process.exitValue());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        open = false;
        HollowFrontier.LOGGER.info("[Trailer] clip done: {} frames written, {} dropped", written, dropped);
    }

    private void write(OutputStream pipe) {
        Frame last = null;
        long next = 0;
        try (pipe) {
            while (true) {
                Frame frame = queue.take();
                long until = frame.rgba() == null ? stopNanos : Math.min(frame.nanos(), stopNanos);
                if (last == null) {
                    if (frame.rgba() == null) {
                        return;
                    }
                    next = frame.nanos();
                } else {
                    // Every output time before this frame shows the one before it
                    while (next < until) {
                        pipe.write(last.rgba());
                        written++;
                        next += period;
                    }
                }
                if (frame.rgba() == null || frame.nanos() >= stopNanos) {
                    return;
                }
                last = frame;
            }
        } catch (IOException e) {
            HollowFrontier.LOGGER.error("[Trailer] lost the pipe to ffmpeg: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
