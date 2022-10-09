package art.lookingup.ntreemap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PaintView extends View {
    Paint defaultPaint, currentPaint, bgPaint, gridPaint, runPaint;
    int width, height;
    // We want to auto-scale our measurement space to our View's pixel width and height.
    // To do so, we need to find the bounds in both X and Z.  We should probably just
    // constrain the space to a square shape since we don't know ahead of time if X
    // or Z will have a larger range.
    int maxX = Integer.MIN_VALUE;
    int maxZ = Integer.MIN_VALUE;
    int minX = Integer.MAX_VALUE;
    int minZ = Integer.MAX_VALUE;

    // Measurements entered so far.
    // TODO(tracy): This will be moved to the main activity for data entry.  For now,
    // just generate some random ints.
    List<Point3D> points = new ArrayList<Point3D>();

    // Leaf positions by run.  leaves[run#][leaf#];
    public Point3D[][] leaves;
    public int currentRun = 1;
    public int currentLeaf = 0;
    public boolean currentRunOnly = true;

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
        //updateBounds();
    }

    @SuppressLint("ResourceAsColor")
    public PaintView(Context context) {
        super(context);

        defaultPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        defaultPaint.setColor(Color.DKGRAY);
        defaultPaint.setStyle(Paint.Style.FILL);

        // Use blue for the current Leaf since we have a green laser.
        currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentPaint.setColor(Color.BLUE);
        currentPaint.setStyle(Paint.Style.FILL);

        // All the points on the currently selected run will be this color.
        runPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        runPaint.setColor(Color.YELLOW);
        runPaint.setStyle(Paint.Style.FILL);

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
        if (maxRange < 100)
            maxRange = 100;
        float scale = (float)width/((float)maxRange*2f);
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

    /**
     * For now we will just compute the bounds on every draw
     */
    public void computeBounds() {
        for (int runNum = 1; runNum <= MainActivity.NUM_RUNS; runNum++) {
            for (int leafNum = 0; leafNum < MainActivity.MAX_LEAVES_PER_RUN; leafNum++) {
                Point3D point = leaves[runNum-1][leafNum];
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
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // We will want to constrain the drawable area to a square to prevent distortion.  So
        // we should use minimum dimension and then pad the other dimension when doing
        // measurement space to screen space coordinate conversion.
        width = getWidth();
        height = getHeight();

        computeBounds();
        canvas.drawColor(Color.BLACK);
        canvas.drawLine(0, height/2, width, height/2, gridPaint);
        canvas.drawLine(width/2, 0, width/2, height, gridPaint);

        /*
        for (Point3D point: points) {
            canvas.drawCircle(toImgSpace(point.x), toImgSpace(point.z), 6, defaultPaint);
        } */
        if (!currentRunOnly) {
            for (int runNum = 1; runNum <= MainActivity.NUM_RUNS; runNum++) {
                // We do a first pass of all runs, skipping the current run and then draw
                // the current run on top later in another pass below.
                if (runNum == currentRun) continue;
                for (int leafNum = 0; leafNum < MainActivity.MAX_LEAVES_PER_RUN; leafNum++) {
                    Point3D leaf = leaves[runNum-1][leafNum];
                    if (leaf.x == 0 && leaf.y == 0 && leaf.z == 0)
                        continue;
                    int imgX = (int) toImgSpace(leaf.x);
                    int imgY = height - (int) toImgSpace(leaf.z);
                    canvas.drawCircle(toImgSpace(leaf.x), imgY, 3, defaultPaint);
                }
            }
        }
        // For the second pass, render the current run only.
        for (int runNum = 1; runNum <= MainActivity.NUM_RUNS; runNum++) {
            if (runNum != currentRun) continue;
            for (int leafNum = 0; leafNum < MainActivity.MAX_LEAVES_PER_RUN; leafNum++) {
                Point3D leaf = leaves[runNum-1][leafNum];
                if (leaf.x == 0 && leaf.y == 0 && leaf.z == 0)
                    continue;
                Paint whichPaint = runPaint;
                if (runNum == currentRun && leafNum == currentLeaf)
                    whichPaint = currentPaint;
                int imgX = (int) toImgSpace(leaf.x);
                int imgY = height - (int) toImgSpace(leaf.z);
                canvas.drawCircle(toImgSpace(leaf.x), imgY, 3, whichPaint);
            }
        }
    }
}
