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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class JvnCoordImpl
        extends UnicastRemoteObject
        implements JvnRemoteCoord, Serializable {

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
        //jvnLoadState();

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
        System.out.println(newObjectId);
        int id = newObjectId++;
        jvnSaveState();
        return id;
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
        int joi = jo.jvnGetObjectId();
        nameIdMap.put(jon, joi);
        idObjectMap.put(joi, jo);
        // When an object is registered, it has a write lock in the server that created it
        writers.put(joi, js);
        jvnSaveState();
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
        int joi = nameIdMap.get(jon);
        System.out.println("JvnCoordImpl: jvnLookupObject: " + jon + " id: " + joi);
        jvnSaveState();
        JvnObject jo = idObjectMap.get(joi);
        // When he looks up an object, he doesnt have a lock on it
        jo.setLockState(lockState.NL);
        return jo;
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
            idObjectMap.put(joi, (JvnObject) writers.get(joi).jvnInvalidateWriterForReader(joi));
            if (writers.get(joi) != js) {
                addReader(joi, writers.get(joi));
            }
            writers.remove(joi);
        }
        addReader(joi, js);
        jvnSaveState();
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

        // Print writers state
        System.out.println("Writers: ");
        for (JvnRemoteServer jrs : writers.values()) {
            System.out.println("    - writer");
        }
        // Print readers state
        System.out.println("Readers: ");
        for (ArrayList<JvnRemoteServer> ral : readers.values()) {
            for (JvnRemoteServer jrs : ral) {
                System.out.println("    - reader");
            }
        }

        if (!idObjectMap.containsKey(joi)) {
            throw new JvnException("Object not found on Lock Write");
        }
        if (writers.containsKey(joi)) {
            if (!writers.get(joi).equals(js)) {
                JvnObject obj = (JvnObject) writers.get(joi).jvnInvalidateWriter(joi);
                idObjectMap.put(joi, obj);
                // writers.get(joi).jvnInvalidateWriter(joi);
                writers.remove(joi);
            }
        }
        // synchro problems
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
        jvnSaveState();
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
        // Loop through the readers map
        readers.forEach((key, value) -> {
            // Remove the server from the list of readers if it exists
            value.remove(js);
        });
        writers.values().remove(js);
        jvnSaveState();
    }

    // Save the JVN server state to a file
    public void jvnSaveState() throws java.rmi.RemoteException, JvnException {
        try {
            File f = new File("jvnCoordState.ser");
            // Create the file if it doesn't exist
            f.createNewFile();
            FileOutputStream fileOut = new FileOutputStream(f);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in jvnCoordState.ser");
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    // Load the JVN server state from a file
    public void jvnLoadState() throws java.rmi.RemoteException, JvnException {
        try {
            File f = new File("jvnCoordState.ser");
            if (!f.isFile() || !f.canRead()) return;
            FileInputStream fileIn = new FileInputStream(f);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            JvnCoordImpl jvnCoord = (JvnCoordImpl) in.readObject();
            in.close();
            fileIn.close();
            this.idObjectMap = jvnCoord.idObjectMap;
            this.nameIdMap = jvnCoord.nameIdMap;
            this.readers = jvnCoord.readers;
            this.writers = jvnCoord.writers;
            System.out.println("JvnCoordImpl: jvnLoadState");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void addReader(int joi, JvnRemoteServer js) {
        if (readers.containsKey(joi)) {
            if (!readers.get(joi).contains(js)) {
                readers.get(joi).add(js);
            }
        } else {
            readers.put(joi, new ArrayList<JvnRemoteServer>());
            readers.get(joi).add(js);
        }
    }

    public static void main(String[] args) throws Exception {
        JvnCoordImpl coord = new JvnCoordImpl();
    }
}