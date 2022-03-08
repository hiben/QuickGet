package de.zvxeb.quickget;

import io.nayuki.qrcodegen.QrCode;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Util {
    public static BufferedImage getImage(QrCode qrCode, Color light, Color dark) {
        if(light == null) {
            light = Color.white;
        }
        if(dark == null) {
            dark = Color.black;
        }
        int size = qrCode.size;
        int cl = light.getRGB();
        int cd = dark.getRGB();
        BufferedImage codeImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int [] line = new int [size];

        for(int y=0; y<size; y++) {
            for(int x=0; x<size; x++) {
                line[x] = qrCode.getModule(x, y) ? cd : cl;
            }
            codeImage.setRGB(0, y, size,1,line,0, size);
        }

        return codeImage;
    }
}
