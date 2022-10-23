package burst;

import jvn.DynamicProxy.JvnOperation;

interface InterfaceBurst {
    @JvnOperation(type = "write")
    public void incrementCounter();

    @JvnOperation(type = "read")
    public int getCounter();
    
}
