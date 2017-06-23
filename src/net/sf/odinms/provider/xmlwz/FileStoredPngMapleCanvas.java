package net.sf.odinms.provider.xmlwz;

import net.sf.odinms.provider.MapleCanvas;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FileStoredPngMapleCanvas implements MapleCanvas {
    private final File file;
    private int width;
    private int height;
    private BufferedImage image;

    public FileStoredPngMapleCanvas(final int width, final int height, final File fileIn) {
        this.width = width;
        this.height = height;
        this.file = fileIn;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public BufferedImage getImage() {
        loadImageIfNecessary();
        return image;
    }
    private void loadImageIfNecessary() {
        if (image == null) {
            try {
                image = ImageIO.read(file);
                // replace the dimensions loaded from the wz by the REAL dimensions from the image - should be equal tho
                width = image.getWidth();
                height = image.getHeight();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
