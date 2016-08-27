package image;

/**
 * Created by itjamal on 8/9/2016.
 * Code used from http://blog.demofox.org/2015/08/15/resizing-images-with-bicubic-interpolation/
 */
public class ImageTransform {

    public enum InterpolationMode {BILINEAR, BICUBIC}

    // this method places the given matrix in the center of a new matrix with given W and H sizes.
    // throws error if new boundary is less than the given matrix
    public double[][] fillMatrix(double[][] source, int width, int height) {
        double[][] newMatrix = new double[width][height];

        if ((source == null) || (source.length == 0) || (source[0].length == 0))
            throw new IllegalArgumentException("Null or empty  matrix given");
        else if (source.length > width)
            throw new IllegalArgumentException("Width cannot be less than the source matrix's width");
        else if (source[0].length > height)
            throw new IllegalArgumentException("Height cannot be less than the source matrix's height");

        int xOffset = (width - source.length) / 2;
        int yOffset = (height - source[0].length) / 2;

        // copy source matrix to the center of the new one
        for (int i = 0; i < source.length; i++)
            for (int j = 0; j < source[0].length; j++)
                newMatrix[i + xOffset][j + yOffset] = source[i][j];
        return newMatrix;
    }

    public double[][] resize(int[][] source, InterpolationMode inMode, int newWidth, int newHeight) {
        double[][] resizedMatrix = new double[newWidth][newHeight];

        for (int i = 0; i < newWidth; i++) {
            double u = (double) i / ((double) newWidth - 1);
            for (int j = 0; j < newHeight; j++) {
                double v = (double) j / ((double) newHeight - 1);
                if (inMode == InterpolationMode.BILINEAR)
                    resizedMatrix[i][j] = sampleBilinear(source, u, v);
                else if (inMode == InterpolationMode.BICUBIC)
                    resizedMatrix[i][j] = sampleBicubic(source, u, v);
            }
        }
        return resizedMatrix;
    }

    public double[][] resizeAndFill(int[][] pixels, InterpolationMode inMode, int newWidth) {
        int w = pixels.length;
        int h = pixels[0].length;

        double dScale = newWidth / Math.max(w, h);
        int wscale, hscale;
        if (w > h) {
            wscale = newWidth;
            hscale = (int) (h * dScale);
        } else {
            hscale = newWidth;
            wscale = (int) (w * dScale);
        }

        ImageTransform imtrans = new ImageTransform();
        double[][] resImg = imtrans.resize(pixels, inMode, wscale, hscale);
        double[][] newImg = imtrans.fillMatrix(resImg, newWidth, newWidth);

        return newImg;
    }

    // this helper function returns an interpolation between two A and B points for parameter t in [0,1]
    public double lerp(double A, double B, double t) {
        return A * (1.0 - t) + B * t;

    }

    public double clamp(double a, double min, double max) {
        if (a < min)
            return min;
        else if (a >= max)
            return max;
        else
            return a;
    }

    public double getPixelClamped(int[][] source, double x, double y) {
        int i = (int) clamp(x, 0, source.length - 1);
        int j = (int) clamp(y, 0, source[0].length - 1);
        return source[i][j];

    }

    // t is a value that goes from 0 to 1 to interpolate in a C1 continuous way across uniformly sampled data points.
// when t is 0, this will return B.  When t is 1, this will return C.  Inbetween values will return an interpolation
// between B and C.  A and B are used to calculate slopes at the edges.
    public double cubicHermite(double A, double B, double C, double D, double t) {
        double a = -A / 2.0 + (3.0 * B) / 2.0 - (3.0 * C) / 2.0 + D / 2.0;
        double b = A - (5.0 * B) / 2.0 + 2.0 * C - D / 2.0;
        double c = -A / 2.0 + C / 2.0;
        double d = B;

        return a * t * t * t + b * t * t + c * t + d;
    }

    public double sampleBilinear(int[][] source, double u, double v) {
        double x = (u * source.length) - 0.5;
        int xint = (int) x;
        double xfract = x - Math.floor(x);

        double y = (v * source[0].length) - 0.5;
        int yint = (int) y;
        double yfract = y - Math.floor(y);

        double pp00 = getPixelClamped(source, xint, yint);
        double pp10 = getPixelClamped(source, xint + 1, yint);
        double pp01 = getPixelClamped(source, xint, yint + 1);
        double pp11 = getPixelClamped(source, xint + 1, yint + 1);

        // first, get horizontal values
        double l1 = lerp(pp00, pp10, xfract);
        double l2 = lerp(pp01, pp11, xfract);

        double value = lerp(l1, l2, yfract);
        double retval = clamp(value, 0.0, 255.0);
        return Math.round(retval);
    }

    public double sampleBicubic(int[][] source, double u, double v) {
        // calculate coordinates -> also need to offset by half a pixel to keep image from shifting down and left half a pixel
        double x = (u * source.length) - 0.5;
        int xint = (int) x;
        double xfract = x - Math.floor(x);

        double y = (v * source[0].length) - 0.5;
        int yint = (int) y;
        double yfract = y - Math.floor(y);


        // 1st row
        double p00 = getPixelClamped(source, xint - 1, yint - 1);
        double p10 = getPixelClamped(source, xint + 0, yint - 1);
        double p20 = getPixelClamped(source, xint + 1, yint - 1);
        double p30 = getPixelClamped(source, xint + 2, yint - 1);

        // 2nd row
        double p01 = getPixelClamped(source, xint - 1, yint + 0);
        double p11 = getPixelClamped(source, xint + 0, yint + 0);
        double p21 = getPixelClamped(source, xint + 1, yint + 0);
        double p31 = getPixelClamped(source, xint + 2, yint + 0);

        // 3rd row
        double p02 = getPixelClamped(source, xint - 1, yint + 1);
        double p12 = getPixelClamped(source, xint + 0, yint + 1);
        double p22 = getPixelClamped(source, xint + 1, yint + 1);
        double p32 = getPixelClamped(source, xint + 2, yint + 1);

        // 4th row
        double p03 = getPixelClamped(source, xint - 1, yint + 2);
        double p13 = getPixelClamped(source, xint + 0, yint + 2);
        double p23 = getPixelClamped(source, xint + 1, yint + 2);
        double p33 = getPixelClamped(source, xint + 2, yint + 2);

        // interpolate bi-cubically!
        // Clamp the values since the curve can put the value below 0 or above 255
        double col0 = cubicHermite(p00, p10, p20, p30, xfract);
        double col1 = cubicHermite(p01, p11, p21, p31, xfract);
        double col2 = cubicHermite(p02, p12, p22, p32, xfract);
        double col3 = cubicHermite(p03, p13, p23, p33, xfract);
        double value = cubicHermite(col0, col1, col2, col3, yfract);

        double retval = clamp(value, 0.0, 255.0);
        return Math.round(retval);
    }

    public static void main(String[] args) {
        int[][] source = {{1, 0, 0, 0, 0, 0, 1, 0},
                {0, 1, 0, 0, 0, 1, 0, 0},
                {0, 0, 1, 0, 1, 0, 0, 0},
                {0, 0, 0, 1, 0, 0, 0, 0},
                {0, 0, 1, 0, 1, 0, 0, 0},
                {0, 1, 0, 0, 0, 1, 0, 0},
                {1, 0, 0, 0, 0, 0, 1, 0}};


        for (int j = 0; j < source[0].length; j++) {
            for (int i = 0; i < source.length; i++) {
                System.out.print(source[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();

        ImageTransform it = new ImageTransform();
        double[][] newImage = it.fillMatrix(it.resize(source, InterpolationMode.BILINEAR, 14, 10), 14, 14);
        //double[][] newImage = it.resize(source, InterpolationMode.BILINEAR, 14, 10);
        for (int j = 0; j < newImage[0].length; j++) {
            for (int i = 0; i < newImage.length; i++) {
                System.out.print((int) newImage[i][j] + " ");
            }
            System.out.println();
        }
    }

}
