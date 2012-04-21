import java.util.ArrayList;
import java.util.Collections;

import processing.core.PApplet;
import processing.core.PImage;
import toxi.geom.Vec3D;

import com.buglabs.bug.jni.camera.Camera;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

@SuppressWarnings("serial")
public class ColorClusters extends PApplet {

  String deviceID = "bugTest";
  String baseURL = "http://localhost:8888/moma/reportColors.php?";

  PImage cam;
  ArrayList<CColor> cols = new ArrayList<CColor>();
  protected static final int PORT = 9000;

  private Thread receiverThread;

  float thresh = 68;
  float bthresh = 160;
  private Camera camera;

  public void setup() {
    size(320, 240);
    smooth();
    System.out.println("getting Pixels");
    getPixels();
    cam = new PImage(320, 240);
    int i = 0;
    for (; i < cam.pixels.length; i++) {
      cam.pixels[i] = 0;
    }
    System.out.println(i);
  }

  public void draw() {

    grabColors();
    semaphore(cols, 320, 240);
    //reportColors();

    @SuppressWarnings("unused")
      float y = 0;
    float s = 10;
    @SuppressWarnings("unused")
      float t = width / s;
  }

  void reportColors() {
    String[] colors = new String[cols.size()];
    for (int i = 0; i < cols.size(); i++) {
      colors[i] = hex(cols.get(i).col, 6);
      System.out.println("reportColors[" + i + "]" + colors[i]);
    }

    String c = join(colors, ",");
    String u = baseURL + "colors=" + c + "&deviceID=" + deviceID;
    println(u);
    loadStrings(u);
  }

  public void mousePressed() {
    line(mouseX, mouseY, pmouseX, pmouseY);
  }

  @SuppressWarnings("unchecked")
    void semaphore(ArrayList<CColor> colorList, float w, float h) {
      Collections.sort(colorList);
      Collections.reverse(colorList);
      if (colorList.size() > 0) {
        float hi = w / (colorList.size());
        @SuppressWarnings("unused")
          float wi = h / (colorList.size());
        float hi2 = w / (colorList.size() - 1);
        float wi2 = h / (colorList.size() - 1);
        noStroke();
        // randomSeed(floor(red(colorList.get(0).col)/10));
        float dice = random(100);
        if (dice < 33) {
          // Circle over stripes
          for (int i = 1; i < colorList.size(); i++) {
            fill(colorList.get(i).col);
            rect((i - 1) * wi2, 0, wi2, h);
          }
          fill(colorList.get(0).col);
          ellipse(w / 2, h / 2, w / 2, w / 2);
        } 
        else if (dice < 66) {
          // Triangles
          fill(colorList.get(0).col);
          rect(0, 0, w, h);
          for (int i = 1; i < colorList.size(); i++) {
            beginShape(TRIANGLES);
            fill(colorList.get(i).col);
            vertex(0, (i - 1) * hi2);
            vertex(0, (i) * hi2);
            vertex(w, (float) ((i - 0.5) * hi2));
            endShape();
          }
        } 
        else if (dice < 100) {
          if (colorList.size() % 2 == 0) {
            // Even number of colors - do a grid
            for (int i = 0; i < colorList.size(); i++) {
              float x = (i % 2) * (w / 2);
              float y = floor(i / 2) * hi * 2;
              fill(colorList.get(i).col);
              rect(x, y, w / 2, hi * 2);
            }
          } 
          else {
            // Odd number of colors - do a stack
            for (int i = 0; i < colorList.size(); i++) {
              float x = 0;
              float y = i * hi;
              fill(colorList.get(i).col);
              rect(x, y, w, hi);
            }
          }
        } 
        else {
        }
      }
    }

  void grabColors() {
    // thresh = 60;//map(mouseY, 0, height, 0, 200);
    // bthresh = 160;//map(mouseX, 0, width, 0, 200);

    /*
		 * println(thresh + ": " + bthresh); int s = millis();
     		 * println("******");
     		 */
    cluster(cam.pixels);
    cleanClusters();
    /*
		 * println(cols.size()); println("******" + (millis() - s));
     		 * println("---");
     		 */
  }

  void getPixels() {
    camera = new Camera();
    camera.bug_camera_open("/dev/media0", -1, 2048, 1536, 320, 240);
    camera.bug_camera_start();
    receiverThread = new Thread() {

      private final BufferedImage image = new BufferedImage(640, 480, 
      BufferedImage.TYPE_INT_ARGB);

      private int[] pixelBuffer = ((DataBufferInt) image.getRaster()
        .getDataBuffer()).getData();

      public void run() {

        while (!interrupted ()) {

          camera.bug_camera_grab_preview(pixelBuffer);

          cam.loadPixels();
          /*for (int layer = 0; layer < 4; layer++) {
           						for (int p = 0; p < pixelBuffer.length / 4; p++) {
           							cam.pixels[p] += ((p & 0xFF) << (8 * layer));
           						}
           					}*/
          cam.pixels = pixelBuffer;
          cam.updatePixels();
        }
      }
    };

    receiverThread.start();
  }

  void cluster(int[] pix) {
    cols = new ArrayList<CColor>();
    for (int i = 0; i < pix.length; i += 10) {
      int c = pix[i];
      if (brightness(c) + saturation(c) > bthresh && saturation(c) > 60
        && brightness(c) > 40) {
        CColor s = stick(c);
        if (s != null) {
          s.cols.add(new CColor(c));
        } 
        else {
          CColor col = new CColor(c);
          col.cols.add(new CColor(c));
          cols.add(col);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
    void cleanClusters() {
      for (CColor c : cols) {
        Collections.sort(c.cols);
        colorMode(HSB);
        c.col = brighten(c.cols.get(0).col);
        colorMode(RGB);
      }
    }

  int brighten(int c) {

    float h = hue(c);
    float s = (float) (saturation(c) + ((255 - saturation(c)) * 0.3));
    float b = (float) (brightness(c) + ((255 - brightness(c)) * 0.3));

    return (color(h, s, b));
  }

  CColor stick(int c) {
    CColor s = null;

    Vec3D cv = new Vec3D(red(c), green(c), blue(c));
    // Vec3D cv = new Vec3D(hue(c),0,0);
    for (CColor col : cols) {
      Vec3D cv2 = new Vec3D(red(col.col), green(col.col), blue(col.col));
      // Vec3D cv2 = new Vec3D(hue(c),0,0);
      if (cv.distanceTo(cv2) < thresh) {
        s = col;
        break;
      }
    }
    return (s);
  }

  @SuppressWarnings("rawtypes")
  class CColor implements Comparable {
    int col;
    ArrayList<CColor> cols = new ArrayList<CColor>();

    CColor(int c) {
      col = c;
    }

    public int compareTo(Object c) {
      CColor cc = (CColor) c;
      return ((int) -((brightness(col) + saturation(col)) - (brightness(cc.col) + saturation(cc.col))));
    }
  };

  public void keyPressed() {
    if (key == '[')
      thresh--;
    if (key == ']')
      thresh++;

    println(thresh);
  }
}
