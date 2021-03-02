package tw.com.flag.TENS_bluetooth;

/**
 * Created by ZRD on 2019/4/13.
 */
public class mODE45 {
    //Lorenz
    public double a = 10;
    public double b = 8/3;
    public double c = 28;
    //lorenz
    public double x = 0;
    public double y = 1;
    public double z = 1;
    public double h = 0.1;
    public mODE45(){
    }
    public void DomODE45(){
        double x1 = dxdt(x, y, z);
        double y1 = dydt(x, y, z);
        double z1 = dzdt(x, y, z);

        double x2 = dxdt(x+h/2*x1, y + h/2*y1 , z + h/2*z1);
        double y2 = dydt(x+h/2*x1, y + h/2*y1 , z + h/2*z1);
        double z2 = dzdt(x+h/2*x1, y + h/2*y1 , z + h/2*z1);

        double x3 = dxdt(x+h/2*x2, y + h/2*y2 , z + h/2*z2);
        double y3 = dydt(x+h/2*x2, y + h/2*y2 , z + h/2*z2);
        double z3 = dzdt(x+h/2*x2, y + h/2*y2 , z + h/2*z2);

        double x4 = dxdt(x+h*x3, y + h*y3 , z + h*z3);
        double y4 = dydt(x+h*x3, y + h*y3 , z + h*z3);
        double z4 = dzdt(x+h*x3, y + h*y3 , z + h*z3);

        x = x + h/6*(x1 + 2*x2 + 2*x3 + x4);
        y = y + h/6*(y1 + 2*y2 + 2*y3 + y4);
        z = z + h/6*(z1 + 2*z2 + 2*z3 + z4);
    }
    //Lorenz
    public double dxdt(double x_, double y_, double z_){
        double function = a * (y_ - x_);
        return function;
    }
    public double dydt(double x_, double y_, double z_){
        double function = c * x_ - y_ - x_ * z_;
        return function;
    }
    public double dzdt(double x_, double y_, double z_){
        double function = x_*y_ - b*z_;
        return function;
    }
}
