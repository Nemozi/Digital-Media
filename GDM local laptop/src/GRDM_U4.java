import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;


public class GRDM_U4 implements PlugInFilter {

    protected ImagePlus imp;
    final static String[] choices = {"Wischen", "Weiche Blende", "Overlay", "Schiebe Blende", "Chroma Key", "ueberrasche mich :)"};

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB + STACK_REQUIRED;
    }

    public static void main(String args[]) {
        ImageJ ij = new ImageJ(); // neue ImageJ Instanz starten und anzeigen
        ij.exitWhenQuitting(true);

        IJ.open("/home/nemo/University Porfolio/GDM 2023/GDM local laptop/StackB.zip");

        GRDM_U4 sd = new GRDM_U4();
        sd.imp = IJ.getImage();
        ImageProcessor B_ip = sd.imp.getProcessor();
        sd.run(B_ip);
    }

    public void run(ImageProcessor B_ip) {
        // Film B wird uebergeben
        ImageStack stack_B = imp.getStack();

        int length = stack_B.getSize();
        int width = B_ip.getWidth();
        int height = B_ip.getHeight();

        // ermoeglicht das Laden eines Bildes / Films
        Opener o = new Opener();
        OpenDialog od_A = new OpenDialog("Auswählen des 2. Filmes ...", "/home/nemo/University Porfolio/GDM 2023/GDM local laptop/StackA.zip");

        // Film A wird dazugeladen
        String dateiA = od_A.getFileName();
        if (dateiA == null) return; // Abbruch
        String pfadA = od_A.getDirectory();
        ImagePlus A = o.openImage(pfadA, dateiA);
        if (A == null) return; // Abbruch

        ImageProcessor A_ip = A.getProcessor();
        ImageStack stack_A = A.getStack();

        if (A_ip.getWidth() != width || A_ip.getHeight() != height) {
            IJ.showMessage("Fehler", "Bildgrößen passen nicht zusammen");
            return;
        }

        // Neuen Film (Stack) "Erg" mit der kleineren Laenge von beiden erzeugen
        length = Math.min(length, stack_A.getSize());

        ImagePlus Erg = NewImage.createRGBImage("Ergebnis", width, height, length, NewImage.FILL_BLACK);
        ImageStack stack_Erg = Erg.getStack();

        // Dialog fuer Auswahl des Ueberlagerungsmodus
        GenericDialog gd = new GenericDialog("Überlagerung");
        gd.addChoice("Methode", choices, "");
        gd.showDialog();

        int methode = 0;
        String s = gd.getNextChoice();
        if (s.equals("Wischen")) methode = 1;
        if (s.equals("Weiche Blende")) methode = 2;
        if (s.equals("Overlay")) methode = 3;
        if (s.equals("Schiebe Blende")) methode = 4;
        if (s.equals("Chroma Key")) methode = 5;
        if (s.equals("ueberrasche mich :)")) methode = 6;


        // Arrays fuer die einzelnen Bilder
        int[] pixels_B;
        int[] pixels_A;
        int[] pixels_Erg;

        // Schleife ueber alle Bilder
        for (int z = 1; z <= length; z++) {
            pixels_B = (int[]) stack_B.getPixels(z);
            pixels_A = (int[]) stack_A.getPixels(z);
            pixels_Erg = (int[]) stack_Erg.getPixels(z);

            int pos = 0;
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++, pos++) {
                    int cA = pixels_A[pos];
                    int rA = (cA & 0xff0000) >> 16;
                    int gA = (cA & 0x00ff00) >> 8;
                    int bA = (cA & 0x0000ff);

                    int cB = pixels_B[pos];
                    int rB = (cB & 0xff0000) >> 16;
                    int gB = (cB & 0x00ff00) >> 8;
                    int bB = (cB & 0x0000ff);

                    if (methode == 1) {
                        if (y + 1 > (z - 1) * (double) height / (length - 1))
                            pixels_Erg[pos] = pixels_B[pos];
                        else
                            pixels_Erg[pos] = pixels_A[pos];

                    }
                    if (methode == 3) {

                        int r = 255 - ((255 - rB) * (255 - rA) / 255);
                        int g = 255 - ((255 - gB) * (255 - gA) / 255);
                        int b = 255 - ((255 - bB) * (255 - bA) / 255);
                        pixels_Erg[pos] = 0xFF000000 + ((r & 0xff) << 16) + ((g & 0xff) << 8) + (b & 0xff);
                    } else if (methode == 2) {
                        double fadeIntensity = 1 - ((double) z / (length - 1));
                        int r = (int) Math.round(rA + (rB - rA) * fadeIntensity);
                        int g = (int) Math.round(gA + (gB - gA) * fadeIntensity);
                        int b = (int) Math.round(bA + (bB - bA) * fadeIntensity);


                        pixels_Erg[pos] = 0xFF000000 + ((r & 0xff) << 16) + ((g & 0xff) << 8) + (b & 0xff);

                    } else if (methode == 4) {
                        int i = ((z - 1) * width / (length - 1));

                        if ((x + 1) <= (z - 1) * (double) width / (length - 1)) {
                            pixels_Erg[pos] = pixels_A[(y * width) + (width - i + x)];
                        } else {
                            pixels_Erg[pos] = pixels_B[(y * width) + (x - i)];
                        }
                    } else if (methode == 5) {
                        if ((rA >= 150 && rA <= 255) && (gA >= 50 && gA <= 200) && (bA >= 0 && bA <= 100)) {
                            pixels_Erg[pos] = pixels_B[pos];
                        } else {
                            pixels_Erg[pos] = pixels_A[pos];
                        }
                    } else if (methode == 6) {
                        int squareSize = width;
                        int centerX = width / 2;
                        int centerY = height / 2;
                        int squareStartX = centerX - squareSize / 2;
                        int squareStartY = centerY - squareSize / 2;
                        int squareEndX = squareStartX + squareSize;
                        int squareEndY = squareStartY + squareSize;

                        int currentSize = squareSize * (z - 1) / (length - 1);
                        int currentStartX = centerX - currentSize / 2;
                        int currentStartY = centerY - currentSize / 2;
                        int currentEndX = currentStartX + currentSize;
                        int currentEndY = currentStartY + currentSize;

                        if (x >= squareStartX && x < squareEndX && y >= squareStartY && y < squareEndY) {
                            if (x >= currentStartX && x < currentEndX && y >= currentStartY && y < currentEndY) {
                                pixels_Erg[pos] = pixels_A[pos];
                            } else {
                                pixels_Erg[pos] = pixels_B[pos];
                            }
                        } else {
                            pixels_Erg[pos] = pixels_B[pos];
                        }
                    }

                    }

                }

            // neues Bild anzeigen
            Erg.show();
            Erg.updateAndDraw();

        }

    }

