import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;


public class Scale implements PlugInFilter {

    public int setup(String arg, ImagePlus imp) {
        if (arg.equals("about"))
        {showAbout(); return DONE;}
        return DOES_RGB+NO_CHANGES;
        // kann RGB-Bilder und veraendert das Original nicht
    }

    public void run(ImageProcessor ip) {

        String[] dropdownmenue = {"Kopie", "Pixelwiederholung", "Bilinear"};

        GenericDialog gd = new GenericDialog("scale");
        gd.addChoice("Methode",dropdownmenue,dropdownmenue[0]);
        gd.addNumericField("Hoehe:",500,0);
        gd.addNumericField("Breite:",400,0);

        gd.showDialog();

        int height_n = (int)gd.getNextNumber(); // _n fuer das neue skalierte Bild
        int width_n =  (int)gd.getNextNumber();

        int width  = ip.getWidth();  // Breite bestimmen
        int height = ip.getHeight(); // Hoehe bestimmen

        //height_n = height;
        //width_n  = width;

        ImagePlus neu = NewImage.createRGBImage("Skaliertes Bild",
                width_n, height_n, 1, NewImage.FILL_BLACK);

        ImageProcessor ip_n = neu.getProcessor();
        String method = dropdownmenue[gd.getNextChoiceIndex()];

        int[] pix = (int[])ip.getPixels();
        int[] pix_n = (int[])ip_n.getPixels();


            // Schleife ueber das neue Bild
            for (int y_n = 0; y_n < height_n; y_n++) {
                for (int x_n = 0; x_n < width_n; x_n++) {
                    int y = y_n;
                    int x = x_n;

                    if(method.equals("Kopie")){
                    if (y < height && x < width) {
                        int pos_n = y_n * width_n + x_n;
                        int pos = y * width + x;

                        pix_n[pos_n] = pix[pos];
                    }
                    }
                    if(method.equals("Pixelwiederholung")){
                        int x_scaled = (int) Math.round(x * (width - 1) / (double) (width_n - 1));
                        int y_scaled = (int) Math.round(y * (height - 1) / (double) (height_n - 1));
                        int pos_n = y_n * width_n + x_n;
                        int pos = y_scaled * width + x_scaled;
                        pix_n[pos_n] = pix[pos];
                    }

                    if(method.equals("Bilinear")){
                        float Sw = (float)width_n / (float)width;
                        float Sh = (float)height_n / (float)height;

                        float xA = x_n / Sw;
                        float yA = y_n / Sh;



                        float h = xA - (int)xA;
                        float v = yA - (int)yA;


                        // get the coordinates for the four squares A,B,C,D
                        int x1 = (int) xA;
                        int y1 = (int) yA;
                        int x2 = Math.min(x1+1, width-1); // here we make sure x2 is never more than width
                        int y2 = Math.min(y1+1, height-1); // make sure y2 is never more than height


                        int Apos = y1*width + x1;

                        int Ar = (pix[Apos] >> 16) & 0xff;
                        int Ag = (pix[Apos] >>  8) & 0xff;
                        int Ab =  pix[Apos]        & 0xff;


                        int Bpos = y2*width + x1;

                        int Br = (pix[Bpos] >> 16) & 0xff;
                        int Bg = (pix[Bpos] >>  8) & 0xff;
                        int Bb =  pix[Bpos]        & 0xff;


                        int Cpos = y2*width + x1;

                        int Cr = (pix[Cpos] >> 16) & 0xff;
                        int Cg = (pix[Cpos] >>  8) & 0xff;
                        int Cb =  pix[Cpos]        & 0xff;


                        int Dpos = y2*width + x2;

                        int Dr = (pix[Dpos] >> 16) & 0xff;
                        int Dg = (pix[Dpos] >>  8) & 0xff;
                        int Db =  pix[Dpos]        & 0xff;


                        // formula from slides to get the color value based on position between 4 pixels
                        int rn = (int)(Ar * (1-h) * (1-v) + Br * h * (1-v) + Cr * (1-h) * v + Dr * h * v);
                        int gn = (int)(Ag * (1-h) * (1-v) + Bg * h * (1-v) + Cg * (1-h) * v + Dg * h * v);
                        int bn = (int)(Ab * (1-h) * (1-v) + Bb * h * (1-v) + Cb * (1-h) * v + Db * h * v);

                        int pos = y_n * width_n + x_n;
                        pix_n[pos] = (0xFF<<24) | (rn<<16) | (gn << 8) | bn;
                    }
                }
            }



        // neues Bild anzeigen
        neu.show();
        neu.updateAndDraw();
    }

    void showAbout() {
        IJ.showMessage("");
    }
}

