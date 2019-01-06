/*
* Nik M
* https://github.com/c-mcg/Web-Crawler
*/
package org.cmcg.webcrawler.search;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.cmcg.logger.Log;

/**
 * A search object that searches for 'img' tags and compares the image to local images.
 */
public abstract class HTMLImageSearch extends XMLTagSearch {

    private String homePage;

    private boolean preloadImages;
    private boolean resizeImages;
    private ArrayList<SearchImage> imagesToFind;
    private HashMap<URL, Integer[]> checkedImages;

    /**
     * 
     * @param homePage The homepage for the hosted images. Skips URLs that don't contain this string.
     * @param preloadImages True if local images should be loaded before processing. False if each local image should be loaded every time it's checked (Easier on memory).
     * @param resizeImages True if site images should be resized before comparison
     * @param imagesToFind The paths for the local images. Directories are searched recursively for images.
     */
    public HTMLImageSearch(String homePage, boolean preloadImages, boolean resizeImages, String... imagesToFind) {
        super("img");
        this.homePage = homePage;
        this.preloadImages = preloadImages;
        this.imagesToFind = new ArrayList<SearchImage>();
        this.checkedImages = new HashMap<URL, Integer[]>();

        ArrayList<String> pathsToFind = new ArrayList<String>();
        for (String s : imagesToFind) {
            pathsToFind.add(s);
        }

        String s;
        while (!pathsToFind.isEmpty()) {
            addPath(pathsToFind.remove(0));
        }
    }

    private void addPath(String s) {
        File file = new File(s);

        if (file.exists()) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    if (f.isDirectory() || f.getName().endsWith(".png") || f.getName().endsWith(".jpg")
                            || f.getName().endsWith(".jpeg") || f.getName().endsWith(".bmp")) {
                        addPath(f.getPath());
                    }
                }
                return;
            }
            
            try {
                this.imagesToFind.add(new SearchImage(s, preloadImages));
            } catch (IOException e) {
                Log.error("Could not load image: " + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    /**
     * Called when a matching image is found
     * @param originalImage The path to the original Image
     * @param matchedImage The url to the matched image
     * @param pageUrl The url the image was found on
     */
    public abstract void onFoundImage(String originalImage, String matchedImage, String pageUrl);

    @Override
    public void onFoundTag(HashMap<String, String> attributes, String tagContents) {
        String src = attributes.get("src");

        if (src == null || !src.contains(homePage) || src.endsWith(".gif")) {
            return;
        }

        src = src.trim();

        if (src.startsWith("\"") || src.startsWith("'")) {
            src = src.substring(1);
        }

        if (src.endsWith("\"") || src.endsWith("'")) {
            src = src.substring(0, src.length() - 1);
        }

        if (src.startsWith("/")) {
            src = "http://" + homePage + src;
        }

        try {
            URL url = new URL(src);
            if (checkedImages.containsKey(url)) {
                for (Integer i : checkedImages.get(url)) {
                    onFoundImage(imagesToFind.get(i).path, src, super.pageUrl);
                }
                return;
            }

            Image siteImage = ImageIO.read(url);

            if (siteImage == null) {
                return;
            }

            int size = imagesToFind.size();

            ArrayList<Integer> matchedLocalImages = new ArrayList<Integer>();
            ImageIcon icon = new ImageIcon(siteImage);
            int h1 = icon.getIconHeight();
            int w1 = icon.getIconWidth();
            
            Log.log("Testing against " + size + " " + (preloadImages ? "" : "non-") + "preloaded images: " + src);

            local_image_loop: for (int i = 0; i < size; i++) {
                SearchImage searchImage = imagesToFind.get(i);
                int[] pixelData = searchImage.getPixelData();
                if (pixelData == null) {
                    Log.error("Pixel data was null for image: " + searchImage.path);
                    continue;
                }
                int h2 = searchImage.height;
                int w2 = searchImage.width;

                if (w1 != w2 || h1 != h2) {
                    if (resizeImages) {
                        siteImage = siteImage.getScaledInstance(w1, h1, Image.SCALE_SMOOTH);
                    } else {
                        continue;//TODO if an image gets skipped it won't get another change to retry atm
                    }
                }

                int[] pixelData2 = new int[w1 * h1];
                PixelGrabber pxGrabber = new PixelGrabber(siteImage, 0, 0, w1, h1, pixelData2, 0, w1);

                try {
                    pxGrabber.grabPixels();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;//TODO here too
                }

                for (int j = 0; j < pixelData.length; j++) {
                    if (pixelData[j] != pixelData2[j]) {
                        continue local_image_loop;
                    }
                }

                matchedLocalImages.add(i);
                onFoundImage(searchImage.path, src, super.pageUrl);
            }

            checkedImages.put(url, matchedLocalImages.toArray(new Integer[matchedLocalImages.size()]));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void reset() {
        super.reset();
    }
    
    private class SearchImage {
        private static final int NUM_RETRIES = 5;

        int width, height;
        String path;
        private BufferedImage image;
        private int[] pixelData;
        private int loadRetries;

        SearchImage(String path, boolean preload) throws IOException {
            this.path = path;

            BufferedImage image = null;

            image = ImageIO.read(new File(path));

            ImageIcon icon = new ImageIcon(image);
            this.width = icon.getIconWidth();
            this.height = icon.getIconHeight();

            if (preload) {
                this.image = image;
                pixelData = new int[width * height];
                PixelGrabber pxGrabber = new PixelGrabber(image, 0, 0, width, height, pixelData, 0, width);
                try {
                    pxGrabber.grabPixels();
                } catch (InterruptedException e) {
                    Log.error("Could not grab pixels from image \"" + this.path + "\"" + Arrays.toString(e.getStackTrace()));
                }
            }
        }

        private BufferedImage getImage() {
            if (image == null && loadRetries < NUM_RETRIES) {
                try {
                    return ImageIO.read(new File(path));
                } catch (IOException e) {
                    loadRetries++;
                    Log.log("Could not load image file \"" + this.path + "\"\nRetry count: " + loadRetries);
                    e.printStackTrace();
                }
            }
            return image;
        }

        int[] getPixelData() {
            if (pixelData == null) {
                pixelData = new int[width * height];
                Image image = getImage();
                if (image != null) {
                    PixelGrabber pxGrabber = new PixelGrabber(image, 0, 0, width, height, pixelData, 0, width);
                    try {
                        pxGrabber.grabPixels();
                    } catch (InterruptedException e) {
                        Log.error("Could not grab pixels from image \"" + this.path + "\"" + Arrays.toString(e.getStackTrace()));
                    }
                }
            }
            return pixelData;
        }
    }

}
