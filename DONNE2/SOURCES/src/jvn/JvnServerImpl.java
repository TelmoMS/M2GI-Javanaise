/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a Jvn server
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.io.*;


public class JvnServerImpl
        extends UnicastRemoteObject
        implements JvnLocalServer, JvnRemoteServer {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    // A JVN server is managed as a singleton
    private static JvnServerImpl js = null;
    private JvnRemoteCoord jrc = null;
    private HashMap<Integer, JvnObject> idObjectMap;

    /**
     * Default constructor
     *
     * @throws JvnException
     **/
    private JvnServerImpl() throws Exception {
        super();
        this.idObjectMap = new HashMap<Integer, JvnObject>();
        Registry registry = LocateRegistry.getRegistry("localhost", 2100);
        this.jrc = (JvnRemoteCoord) registry.lookup("JvnCoord");
    }

    /**
     * Static method allowing an application to get a reference to
     * a JVN server instance
     *
     * @throws JvnException
     **/
    public static JvnServerImpl jvnGetServer() {
        if (js == null) {
            try {
                js = new JvnServerImpl();
            } catch (Exception e) {
                return null;
            }
        }
        return js;
    }

    /**
     * The JVN service is not used anymore
     *
     * @throws JvnException
     **/
    public void jvnTerminate()
            throws jvn.JvnException {
        try {
            jrc.jvnTerminate(this);
        } catch (RemoteException e) {
            throw new JvnException("Remote exception terminating server");
        }
    }

    /**
     * creation of a JVN object
     *
     * @param o : the JVN object state
     * @throws JvnException
     **/
    public JvnObject jvnCreateObject(Serializable o)
            throws jvn.JvnException {
        // to be completed
        int newObjectId;
        try {
            newObjectId = jrc.jvnGetObjectId();
            JvnObject jo = new JvnObjectImpl(newObjectId, o);
            idObjectMap.put(newObjectId, jo);
            return jo;
        } catch (RemoteException e) {
            throw new JvnException("Remote exception creating object");
        }
    }

    /**
     * Associate a symbolic name with a JVN object
     *
     * @param jon : the JVN object name
     * @param jo  : the JVN object
     * @throws JvnException
     **/
    public void jvnRegisterObject(String jon, JvnObject jo)
            throws jvn.JvnException {
        try {
            jrc.jvnRegisterObject(jon, jo, this);
        } catch (RemoteException e) {
            throw new JvnException("Remote exception registering object");
        }
    }

    /**
     * Provide the reference of a JVN object beeing given its symbolic name
     *
     * @param jon : the JVN object name
     * @return the JVN object
     * @throws JvnException
     **/
    public JvnObject jvnLookupObject(String jon)
            throws jvn.JvnException {
        try {
            JvnObject jo = jrc.jvnLookupObject(jon, this);
            if (jo == null) {
                return null;
            } else {
                int objectId = jo.jvnGetObjectId();
                if (!idObjectMap.containsKey(objectId)) {
                    idObjectMap.put(objectId, jo);
                }
                return jo;
            } 
        } catch (RemoteException e) {
            throw new JvnException("Remote exception looking up object");
        }
    }

    /**
     * Get a Read lock on a JVN object
     *
     * @param joi : the JVN object identification
     * @return the current JVN object state
     * @throws JvnException
     **/
    public Serializable jvnLockRead(int joi)
            throws JvnException {
        try {
            return jrc.jvnLockRead(joi, this);
        } catch (RemoteException e) {
            System.out.println(e.getMessage());
            throw new JvnException("Remote exception locking read");
        }
    }

    /**
     * Get a Write lock on a JVN object
     *
     * @param joi : the JVN object identification
     * @return the current JVN object state
     * @throws JvnException
     **/
    public Serializable jvnLockWrite(int joi)
            throws JvnException {
        try {
            return jrc.jvnLockWrite(joi, this);
        } catch (RemoteException e) {
            System.out.println(e.getMessage());
            throw new JvnException("Remote exception locking write");
        }
    }


    /**
     * Invalidate the Read lock of the JVN object identified by id
     * called by the JvnCoord
     *
     * @param joi : the JVN object id
     * @return void
     * @throws java.rmi.RemoteException,JvnException
     **/
    public void jvnInvalidateReader(int joi)
            throws java.rmi.RemoteException, jvn.JvnException {
        if (idObjectMap.containsKey(joi)) {
            JvnObject jo = idObjectMap.get(joi);
            jo.jvnInvalidateReader();
        } else {
            throw new JvnException("InvalidateReader error");
        }
    }

    /**
     * Invalidate the Write lock of the JVN object identified by id
     *
     * @param joi : the JVN object id
     * @return the current JVN object state
     * @throws java.rmi.RemoteException,JvnException
     **/
    public Serializable jvnInvalidateWriter(int joi)
            throws java.rmi.RemoteException, jvn.JvnException {
        if (idObjectMap.containsKey(joi)) {
            JvnObject jo = idObjectMap.get(joi);
            //jo.jvnInvalidateWriter();
            //return jo.jvnGetSharedObject();
            return jo.jvnInvalidateWriter();
        }
        return null;
    }

    /**
     * Reduce the Write lock of the JVN object identified by id
     *
     * @param joi : the JVN object id
     * @return the current JVN object state
     * @throws java.rmi.RemoteException,JvnException
     **/
    public Serializable jvnInvalidateWriterForReader(int joi)
            throws java.rmi.RemoteException, jvn.JvnException {
        if (idObjectMap.containsKey(joi)) {
            JvnObject jo = idObjectMap.get(joi);
            return jo.jvnInvalidateWriterForReader();
            //return jo.jvnGetSharedObject();
        }
        return null;
    }



public static void main(String[] args) {
        try {
            JvnServerImpl js = JvnServerImpl.jvnGetServer();
            System.out.println("Local server ready");
        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
        }
    }

}