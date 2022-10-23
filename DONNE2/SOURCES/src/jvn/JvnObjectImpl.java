package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
    private int id;
    private Serializable object;
    private lockState state;

    public JvnObjectImpl(int id, Serializable object) {
        this.id = id;
        this.object = object;
        this.state = lockState.NL;
    }

    // Constructor for when the object is created, set the lock to WLT
    public JvnObjectImpl(int id, Serializable object, lockState state) {
        this.id = id;
        this.object = object;
        this.state = state;
    }

    @Override
    public int jvnGetObjectId() throws JvnException {
        return id;
    }

    @Override
    public Serializable jvnGetSharedObject() throws JvnException {
        return object;
    }

    @Override
    public synchronized void jvnInvalidateReader() throws JvnException {
        switch (state) {
            // If the lock is RLT or RLT_WLC wait for the read lock to be cached/released
            case RLT, RLT_WLC:
                try {
                    wait();
                    System.out.println(state + " -> NL");
                    state = lockState.NL;
                } catch (InterruptedException e) {
                    throw new JvnException("Interrupted while waiting for lock");
                }
                break;
            default:
                System.out.println(state + " -> NL");
                state = lockState.NL;
                break;
        }
    }

    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        switch (state) {
            // If the lock is being used, wait for the write lock to be cached/released
            case WLT, RLT_WLC:
                try {
                    wait();
                    System.out.println(state + " -> NL");
                    state = lockState.NL;
                } catch (InterruptedException e) {
                    throw new JvnException("Interrupted while waiting for lock");
                }
                break;
            default:
                System.out.println(state + " -> NL");
                state = lockState.NL;
                break;
        }
        return this;
    }

    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        switch (state) {
            case WLT, RLT_WLC:
                try {
                    wait();
                    System.out.println(state + " -> RLC");
                    state = lockState.RLC;
                } catch (InterruptedException e) {
                    throw new JvnException("Interrupted while waiting for lock");
                }
                break;
            default:
                System.out.println(state + " -> RLC");
                state = lockState.RLC;
                break;
        }
        return this;
    }

    @Override
    public synchronized void jvnLockRead() throws JvnException {
        System.out.println("Locking read...");
        switch (state) {
            case RLC:
                state = lockState.RLT;
                System.out.println("RLC -> RLT");
                break;
            // If we have the write lock cached, we can use it to read
            case WLC:
                state = lockState.RLT_WLC;
                System.out.println("WLC -> RLT_WLC");
                break;
            case NL:
                JvnServerImpl js = JvnServerImpl.jvnGetServer();
                object = js.jvnLockRead(id);
                System.out.println("NL -> RLT");
                state = lockState.RLT;
                break;
            case RLT:
                System.out.println("Lock already taken doing nothing");
                break;
            default:
                throw new JvnException("Invalid lock state : " + state);
        }
    }

    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        System.out.println("Locking write...");
        switch (state) { 
            case WLT, WLC, RLT_WLC:
                System.out.println(state + " -> WLT");
                state = lockState.WLT;
                break;
            default:
                System.out.println(state + " -> WLT");
                JvnServerImpl js = JvnServerImpl.jvnGetServer();
                object = js.jvnLockWrite(id);
                state = lockState.WLT;
                break;
        }
    }

    @Override
    public synchronized void jvnUnlock() throws JvnException {
        switch (state) {
            case RLT:
                state = lockState.RLC;
                System.out.println("RLT -> RLC");
                break;
            case WLT:
                state = lockState.WLC;
                System.out.println("WLT -> WLC");
                break;
            case RLT_WLC:
                state = lockState.WLC;
                System.out.println("RLT_WLC -> WLC");
                break;
            default:
        }
        notify();
    }

    @Override
    public void setLockState(lockState state) {
        this.state = state;
    }
}