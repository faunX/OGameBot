package utilities;

public class Timer{
    public static void main(String[] args){
        Timer t = new Timer().start();
        for(int i = 0; i<100; i++);
        t.stop();
        System.out.println(t);
    }

    private long start, end;
    private boolean isStop, isStart;

    public void setStart(long start){
        this.start = start;
    }public void setEnd(long end){
        this.end = end;
    }

    public boolean getIsStop(){return isStop;}
    public boolean getIsStart(){return isStart;}

    public Timer start() {
        start = System.nanoTime();
        isStart = true;
        isStop = false;
        return this;
    }

    public Timer stop() {
        end = System.nanoTime();
        isStart = false;
        isStop = true;
        return this;
    }

    public String toString(){
        return (end-start)+"";
    }

    public long getNanoTime(){
        return (end-start);
    }

    public String getTime(){
        return getTime(getNanoTime());
    }

    public static String getTime(double time) {
        String unit = " nano seconds";
        if(time>=1000){
            time/=1000; unit = " micro seconds";
        }else{
            return time+unit;
        }if(time>=1000){
            time/=1000; unit = " milli seconds";
        }else{
            return time+unit;
        }if(time>=1000){
            time/=1000; unit = " seconds";
        }else{
            return time+unit;
        }if(time>=60){
            time/=60; unit = " minutes";
        }else{
            return time+unit;
        }if(time>=60){
            time/=60; unit = " hours";
        }else{
            return time+unit;
        }if(time>=24){
            time/=24; unit = " days";
        }else{
            return time+unit;
        }if(time>=7){
            time/=7; unit = " weeks";
        }
        return time+unit;
    }
}
