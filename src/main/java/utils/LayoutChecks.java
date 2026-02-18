package utils;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class LayoutChecks {

    public static List<Rect> detectBoxes(Mat bgr) {
        Mat gray = new Mat();
        Imgproc.cvtColor(bgr, gray, Imgproc.COLOR_BGR2GRAY);

        Imgproc.GaussianBlur(gray, gray, new Size(3, 3), 0);

        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 50, 150);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(
                edges,
                contours,
                new Mat(),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
        );

        List<Rect> rects = new ArrayList<>();
        for (MatOfPoint c : contours) {
            Rect r = Imgproc.boundingRect(c);
            if (r.area() > 200) {
                rects.add(r);
            }
        }

        return rects;
    }

    public static int pixelDistance(Rect a, Rect b) {
        return (b.x - (a.x + a.width));
    }
}
