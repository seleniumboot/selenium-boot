package com.seleniumboot.recording;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Pure-Java animated GIF encoder — no external dependencies.
 * Uses {@code javax.imageio} GIF writer with per-frame metadata.
 */
class GifEncoder {

    /**
     * Writes a list of {@link BufferedImage} frames to an animated GIF file.
     *
     * @param frames      ordered list of frames (at least one)
     * @param output      destination file
     * @param delayMillis delay between frames in milliseconds
     */
    static void write(List<BufferedImage> frames, File output, int delayMillis) throws IOException {
        if (frames == null || frames.isEmpty()) return;

        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            writer.prepareWriteSequence(null);

            // delay is in 1/100 of a second (centiseconds)
            int delayCs = Math.max(1, delayMillis / 10);

            for (int i = 0; i < frames.size(); i++) {
                BufferedImage frame = toCompatible(frames.get(i));
                ImageWriteParam      params = writer.getDefaultWriteParam();
                ImageTypeSpecifier   type   = ImageTypeSpecifier.createFromBufferedImageType(frame.getType());
                IIOMetadata          meta   = writer.getDefaultImageMetadata(type, params);

                applyGifMetadata(meta, delayCs, i == 0);
                writer.writeToSequence(new IIOImage(frame, null, meta), params);
            }

            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void applyGifMetadata(IIOMetadata meta, int delayCs, boolean isFirst)
            throws IOException {
        String format = meta.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);

        // Frame delay + disposal
        IIOMetadataNode gce = getOrCreate(root, "GraphicControlExtension");
        gce.setAttribute("disposalMethod",       "restoreToBackgroundColor");
        gce.setAttribute("userInputFlag",         "FALSE");
        gce.setAttribute("transparentColorFlag",  "FALSE");
        gce.setAttribute("delayTime",             String.valueOf(delayCs));
        gce.setAttribute("transparentColorIndex", "0");

        // Loop forever (only set on first frame)
        if (isFirst) {
            IIOMetadataNode appExts = getOrCreate(root, "ApplicationExtensions");
            IIOMetadataNode appExt  = new IIOMetadataNode("ApplicationExtension");
            appExt.setAttribute("applicationID",    "NETSCAPE");
            appExt.setAttribute("authenticationCode", "2.0");
            appExt.setUserObject(new byte[]{0x1, 0x0, 0x0}); // loop infinitely
            appExts.appendChild(appExt);
        }

        meta.setFromTree(format, root);
    }

    /** GIF supports max 256 colors — convert to indexed TYPE_BYTE_INDEXED. */
    private static BufferedImage toCompatible(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_BYTE_INDEXED) return src;
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_BYTE_INDEXED);
        dst.createGraphics().drawImage(src, 0, 0, null);
        return dst;
    }

    private static IIOMetadataNode getOrCreate(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }
}
