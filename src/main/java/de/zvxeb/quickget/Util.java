/**
 QuickGet
 Copyright 2022 Hendrik Iben

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
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
