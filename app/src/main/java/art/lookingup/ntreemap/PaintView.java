package art.lookingup.ntreemap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PaintView extends View {
    Paint defaultPaint, currentPaint, bgPaint, gridPaint;
    int width, height;
    // We want to auto-scale our measurement space to our View's pixel width and height.
    // To do so, we need to find the bounds in both X and Z.  We should probably just
    // constrain the space to a square shape since we don't know ahead of time if X
    // or Z will have a larger range.
    int maxX = Integer.MIN_VALUE;
    int maxZ = Integer.MIN_VALUE;
    int minX = Integer.MAX_VALUE;
    int minZ = Integer.MAX_VALUE;

    static public class Point3D {
        public int x, y, z;

        public Point3D(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
        }
    }

    // Measurements entered so far.
    // TODO(tracy): This will be moved to the main activity for data entry.  For now,
    // just generate some random ints.
    List<Point3D> points = new ArrayList<Point3D>();

    // Create some test data.
    public void generateRandomPoints(int numPoints, int radius) {
        while (points.size() < numPoints) {
            int rx = 2 * ThreadLocalRandom.current().nextInt(radius) - radius;
            int ry = 2 * ThreadLocalRandom.current().nextInt(radius) - radius;
            int rz = 2 * ThreadLocalRandom.current().nextInt(radius) - radius;
            if (Math.sqrt(rx * rx + ry * ry + rz * rz) > radius)
                continue;
            else
                addPoint(new Point3D(rx, ry, rz));
        }
    }

    public void addPoint(Point3D point) {
        points.add(point);
        updateBounds();
    }

    @SuppressLint("ResourceAsColor")
    public PaintView(Context context) {
        super(context);

        defaultPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        defaultPaint.setColor(Color.RED);
        defaultPaint.setStyle(Paint.Style.FILL);

        // Use blue for the current Leaf since we have a green laser.
        currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentPaint.setColor(Color.BLUE);
        currentPaint.setStyle(Paint.Style.FILL);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.BLACK);
        bgPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.LTGRAY);
        bgPaint.setStyle(Paint.Style.STROKE);

        generateRandomPoints(100, 120);
    }

    /**
     * Converts the inches measurement to to image coordinate space.  Image
     * coordinate space starts with 0,0 at the top left.  0,0 in world space
     * is width/2, height/2.
     * @param coordVal
     * @return
     */
    public float toImgSpace(int coordVal) {
        int xRange = maxX - minX;
        int zRange = maxZ - minZ;
        int maxRange = (xRange>zRange)?xRange:zRange;
        // 0,0 is width/2, height/2.
        float scale = width/maxRange;
        float imgCoord = coordVal * scale + width/2;
        return imgCoord;
    }

    /**
     * Each time we add a point, we need to call updateBounds to recompute our
     * bounds.
     */
    public void updateBounds() {
        for (Point3D point : points) {
            if (point.x < minX)
                minX = point.x;
            if (point.x > maxX)
                maxX = point.x;
            if (point.z < minZ)
                minZ = point.z;
            if (point.z > maxZ)
                maxZ = point.z;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // We will want to constrain the drawable area to a square to prevent distortion.  So
        // we should use minimum dimension and then pad the other dimension when doing
        // measurement space to screen space coordinate conversion.
        width = getWidth();
        height = getHeight();

        canvas.drawColor(Color.BLACK);
        canvas.drawLine(0, height/2, width, height/2, gridPaint);

        for (Point3D point: points) {
            canvas.drawCircle(toImgSpace(point.x), toImgSpace(point.z), 6, defaultPaint);
        }
    }
}
