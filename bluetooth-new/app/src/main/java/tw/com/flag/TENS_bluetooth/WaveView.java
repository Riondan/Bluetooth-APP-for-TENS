package tw.com.flag.TENS_bluetooth;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/**
 * Created by ZRD on 2019/4/3.
 */
public class WaveView extends View {
    public int width;
    public int height;
    public Bitmap backBitmap;
    public Canvas canvasBackground;
    public int bx;
    public int by;
    float SampleV=4096+30;
    int SampleV_p;
    int last_x=0;
    int last_y=0;
    int x_step=4;
    Paint paint = new Paint();
    Paint bmpPaint = new Paint();
    Bitmap machineBitmap;
    Bitmap cacheBitmap;
    Canvas machineCanvas = new Canvas();
    public WaveView(Context context, int width, int height) {
        super(context);
        int tmp_w = width;
        int tmp_h = height;
        SampleV_p = tmp_h-50;
        this.height = tmp_h;
        this.width = tmp_w;
        bx = tmp_w;
        by = SampleV_p;
        last_y=tmp_h;
    }
    public void init(){
        drawBackGrid();
        machineBitmap = Bitmap.createBitmap(backBitmap,0,0, this.width, this.height);
        cacheBitmap = Bitmap.createBitmap(backBitmap,0,0, this.width, this.height);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        machineCanvas=new Canvas(machineBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawBitmap(machineBitmap,0,0,bmpPaint);
    }

    private void drawBackGrid()
    {
        int m, n;
        backBitmap= Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvasBackground=new Canvas();
        canvasBackground.setBitmap(backBitmap);
        Paint paint1=new Paint();

        paint1.setColor(Color.WHITE);
        paint1.setStyle(Paint.Style.FILL);
        paint1.setStrokeWidth(2);
        paint1.setAntiAlias(true);
        paint1.setDither(true);
        paint1.setStrokeJoin(Paint.Join.ROUND);

        canvasBackground.drawRect(0, 0, bx, by, paint1);
        paint1.setStyle(Paint.Style.STROKE);
        paint1.setColor(Color.argb(128, 0, 176, 240));
        for (m = 0; m < bx; m = m + 10)
            for (n = 0; n < by; n = n + 10)
                canvasBackground.drawPoint(m, n, paint1);
        for (m = 0; m < bx; m = m + 50)
            canvasBackground.drawLine(m, 0, m, by-5, paint1);
        for (n = 0; n < by; n = n + 50)
            canvasBackground.drawLine(0, n, bx, n,paint1);
    }

    public void drawWave(int real_y){
        float cur_y_p = (float) SampleV_p * (float)real_y / SampleV;
        machineCanvas.drawLine(last_x,last_y,last_x+x_step,(int)cur_y_p,paint);
        last_x = last_x + x_step;
        last_y = (int) cur_y_p;
        ScreenResh();
        this.invalidate();
    }
    private void ScreenResh(){
        Rect rect;
        if (last_x > bx - 5)
        {
            last_x = 0;
            rect=new Rect(0, 0, 20, height);
        }else{
            rect=new Rect(last_x + x_step, 0, last_x + x_step+10, height);
        }
        machineCanvas.drawBitmap(cacheBitmap, rect, rect, bmpPaint);
    }
}
