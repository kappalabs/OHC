package client.ohunter.fojjta.cekuj.net.ohunter;

import android.graphics.Bitmap;

import com.kappa_labs.ohunter.lib.entities.SImage;

/**
 * Created by kappa on 11.1.16.
 */
public class Utils {

    public static Bitmap toBitmap(SImage sImage) {
        byte[] imgBytes = sImage.getImage();
        int[] pixels = new int[sImage.getWidth() * sImage.getHeight()];
        for (int i = 0; i < pixels.length; i++) {
            int byteIndex = i * 3;
            pixels[i] = 0xFF << 24
                    | ((imgBytes[byteIndex + 2] & 0xFF) << 16)
                    | ((imgBytes[byteIndex + 1] & 0xFF) <<  8)
                    |  (imgBytes[byteIndex] & 0xFF);
        }

        return Bitmap.createBitmap(pixels, sImage.getWidth(), sImage.getHeight(), Bitmap.Config.ARGB_8888);
    }

}
