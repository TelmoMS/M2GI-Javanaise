package irc;

import jvn.DynamicProxy.JvnOperation;

interface InterfaceSentence {

    @JvnOperation(type = "write")
    public void write(String text);

    @JvnOperation(type = "read")
    public String read();
    
}
