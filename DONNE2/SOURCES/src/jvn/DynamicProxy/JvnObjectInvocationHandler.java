package jvn.DynamicProxy;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import jvn.JvnObject;
import jvn.JvnServerImpl;

// https://www.baeldung.com/java-dynamic-proxies
// https://www.baeldung.com/java-custom-annotation
public class JvnObjectInvocationHandler implements InvocationHandler {

    private JvnObject jo;

    public JvnObjectInvocationHandler(JvnObject jo) {
        this.jo = jo;
    }

    // Takes a already registered javanaise object and returns the dynamic proxy
    public static Object newInstance(JvnObject jo) throws Exception {
        return java.lang.reflect.Proxy.newProxyInstance(
                jo.jvnGetSharedObject().getClass().getClassLoader(),
                jo.jvnGetSharedObject().getClass().getInterfaces(),
                new JvnObjectInvocationHandler(jo));
    }

    // Takes a string and an object and registers it in the Coordinator if needed
    // returns the dynamic proxy of the object
    public static Object newInstance(Object obj, String remoteObjectName) throws Exception {
        // initialize JVN
        JvnServerImpl js = JvnServerImpl.jvnGetServer();

        // look up the IRC object in the JVN server
        // if not found, create it, and register it in the JVN server
        JvnObject jo = js.jvnLookupObject("IRC");

        if (jo == null) {
            jo = js.jvnCreateObject((Serializable) obj);
            // after creation, I have a write lock on the object
            jo.jvnUnlock();
            js.jvnRegisterObject("IRC", jo);
        }

        return java.lang.reflect.Proxy.newProxyInstance(
                jo.jvnGetSharedObject().getClass().getClassLoader(),
                jo.jvnGetSharedObject().getClass().getInterfaces(),
                new JvnObjectInvocationHandler(jo));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;

        try {
            System.out.println("Invoked method: {" + method.getName() + "}");
            //System.out.println("    - annotation : " + method.getAnnotation(JvnOperation.class));

            if (method.getAnnotation(JvnOperation.class) != null) {
                JvnOperation jvnOperation = method.getAnnotation(JvnOperation.class);

                System.out.println("    - jvnOperation type : " + jvnOperation.type());

                if (jvnOperation.type().equals("read")) {
                    jo.jvnLockRead();
                } else if (jvnOperation.type().equals("write")) {
                    jo.jvnLockWrite();
                }
            }

            // Set the method to accessible
            method.setAccessible(true);
            result = method.invoke(jo.jvnGetSharedObject(), args);

            jo.jvnUnlock();

        } catch (Exception e) {
            System.out.println("Exception in InvocationHandler : " + e.getMessage());
        }
        return result;
    }

}