/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */

package jvn;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.io.Serializable;
import java.util.ArrayList;

public class JvnCoordImpl
        extends UnicastRemoteObject
        implements JvnRemoteCoord {


    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private HashMap<Integer, JvnRemoteServer> writers;
    private HashMap<Integer, ArrayList<JvnRemoteServer>> readers;
    private HashMap<String, Integer> nameIdMap;
    private HashMap<Integer, JvnObject> idObjectMap;
    private int newObjectId;

    /**
     * Default constructor
     *
     * @throws JvnException
     **/
    private JvnCoordImpl() throws Exception {
        super();
        this.nameIdMap = new HashMap<String, Integer>();
        this.idObjectMap = new HashMap<Integer, JvnObject>();
        this.writers = new HashMap<Integer, JvnRemoteServer>();
        this.readers = new HashMap<Integer, ArrayList<JvnRemoteServer>>();
        this.newObjectId = 0;

        Registry r = LocateRegistry.createRegistry(2100);
        r.bind("JvnCoord", this);
        System.out.println("JvnCoordImpl created");
    }

    /**
     * Allocate a NEW JVN object id (usually allocated to a
     * newly created JVN object)
     *
     * @throws java.rmi.RemoteException,JvnException
     **/
    public synchronized int jvnGetObjectId()
            throws java.rmi.RemoteException, jvn.JvnException {
        // to be completed
        return newObjectId++;
    }

    /**
     * Associate a symbolic name with a JVN object
     *
     * @param jon : the JVN object name
     * @param jo  : the JVN object
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
            throws java.rmi.RemoteException, jvn.JvnException {
        System.out.println("JvnCoordImpl: jvnRegisterObject"); 
        if (nameIdMap.containsKey(jon)) {
            throw new JvnException("Object already registered");
        }
        int id = jo.jvnGetObjectId();
        nameIdMap.put(jon, id);
        idObjectMap.put(id, jo);
    }

    /**
     * Get the reference of a JVN object managed by a given JVN server
     *
     * @param jon : the JVN object name
     * @param js  : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
            throws java.rmi.RemoteException, jvn.JvnException {
                
        if (!nameIdMap.containsKey(jon)) {
            return null;
        }
        int idObject = nameIdMap.get(jon);
        System.out.println("JvnCoordImpl: jvnLookupObject: " + jon + " id: " + idObject);
        return idObjectMap.get(idObject);
    }

    /**
     * Get a Read lock on a JVN object managed by a given JVN server
     *
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        System.out.println("JvnCoordImpl: jvnLockRead: " + joi);
        if (!idObjectMap.containsKey(joi)) {
            throw new JvnException("Object not found on Lock Read");
        }
        if (writers.containsKey(joi)) {
            if (!writers.get(joi).equals(js)) {
                JvnObject obj = (JvnObject) writers.get(joi).jvnInvalidateWriterForReader(joi);
                idObjectMap.put(joi, obj);
                //writers.get(joi).jvnInvalidateWriterForReader(joi);
                writers.remove(joi);
            }
            return idObjectMap.get(joi).jvnGetSharedObject();
        }
        if (readers.containsKey(joi)) {
            if (!readers.get(joi).contains(js)) {
                readers.get(joi).add(js);
            }
        } else {
            readers.put(joi, new ArrayList<JvnRemoteServer>());
            readers.get(joi).add(js);
        }
        return idObjectMap.get(joi).jvnGetSharedObject();
    }

    /**
     * Get a Write lock on a JVN object managed by a given JVN server
     *
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        System.out.println("JvnCoordImpl: jvnLockWrite: " + joi); 
        if (!idObjectMap.containsKey(joi)) {
            throw new JvnException("Object not found on Lock Write");
        }
        if (writers.containsKey(joi)) {
            if (!writers.get(joi).equals(js)) {
                idObjectMap.put(joi,(JvnObject)writers.get(joi).jvnInvalidateWriter(joi));
                //writers.get(joi).jvnInvalidateWriter(joi);
                writers.remove(joi);
                writers.put(joi, js);
            }
            return idObjectMap.get(joi).jvnGetSharedObject();
        }
        //synchro problems
        if (readers.containsKey(joi)) {
            for (JvnRemoteServer reader : readers.get(joi)) {
                // Don't invalidate reader if it's the server asking for the write lock
                if (!reader.equals(js)) {
                    reader.jvnInvalidateReader(joi);
                }
            }
            readers.remove(joi);
        }
        writers.put(joi, js);
        return idObjectMap.get(joi).jvnGetSharedObject();
    }

    /**
     * A JVN server terminates
     *
     * @param js : the remote reference of the server
     * @throws java.rmi.RemoteException, JvnException
     **/
    public synchronized void jvnTerminate(JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        //readers.values().remove(js);
        writers.values().remove(js);
    }

    public static void main(String[] args) throws Exception {
        JvnCoordImpl coord = new JvnCoordImpl();
    }
}