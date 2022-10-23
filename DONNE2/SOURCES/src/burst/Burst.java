package burst;

import jvn.DynamicProxy.JvnObjectInvocationHandler;

public class Burst implements java.io.Serializable, InterfaceBurst {
    private int counter;

    public Burst() {
        this.counter = 0;
    }

    @Override
    public void incrementCounter() {
        this.counter += 1;
    }

    @Override
    public int getCounter() {
        return this.counter;
    }


    public static void main(String[] args) throws Exception {
        InterfaceBurst IBurst = (InterfaceBurst) JvnObjectInvocationHandler.newInstance(new Burst(), "Burst");
        // Loop to increment the counter 1000 times and print the result each time
        for (int i = 0; i < 1000; i++) {
            IBurst.incrementCounter();
            // Add a sleep of 10ms to avoid the counter to be incremented too fast
            Thread.sleep(10);
            System.out.println("Counter: " + IBurst.getCounter());
        }
    }

    
}
