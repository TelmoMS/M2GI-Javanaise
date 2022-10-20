package jvn.DynamicProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import jvn.JvnObject;

// https://www.baeldung.com/java-dynamic-proxies
// https://www.baeldung.com/java-custom-annotation
public class JvnObjectInvocationHandler implements InvocationHandler {

    private JvnObject jo;

    public JvnObjectInvocationHandler(JvnObject jo) {
        this.jo = jo;
    }

    public static Object newInstance(JvnObject jo) throws Exception {
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
            System.out.println("    - annotation : " + method.getAnnotation(JvnOperation.class));

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
            result = method.invoke(jo.jvnGetSharedObject(),args);

            jo.jvnUnlock();

        } catch (Exception e) {
            System.out.println("Exception in InvocationHandler : " + e.getMessage());
        }
        return result;
    }

}