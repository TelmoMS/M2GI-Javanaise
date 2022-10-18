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
            case RLT:
                try {
                    wait();
                    state = lockState.NL;
                } catch (InterruptedException e) {
                    throw new JvnException("Interrupted while waiting for lock");
                }
                break;
            default:
                state = lockState.NL;
                break;
        }
    }

    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        switch (state) {
            case WLT, RLT_WLC:
                try {
                    wait();
                    state = lockState.NL;
                } catch (InterruptedException e) {
                    throw new JvnException("Interrupted while waiting for lock");
                }
                break;
            default:
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
                    state = lockState.RLT;
                } catch (InterruptedException e) {
                    throw new JvnException("Interrupted while waiting for lock");
                }
                break;
            default:
                state = lockState.RLT;
                break;
        }
        return this;
    }

    @Override
    public synchronized void jvnLockRead() throws JvnException {
        switch (state) {
            case RLC:
                /*JvnServerImpl js1 = JvnServerImpl.jvnGetServer();
                object = js1.jvnLockRead(id);*/
                state = lockState.RLT;
                break;
            case WLC:
                state = lockState.RLT_WLC;
                break;
            case NL:
                JvnServerImpl js = JvnServerImpl.jvnGetServer();
                object = js.jvnLockRead(id);
                state = lockState.RLT;
                break;
            case RLT:
                break;
            default:
                throw new JvnException("Invalid lock state : " + state);
        }
        System.out.println("Read lock taken");
    }

    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        switch (state) {
            case WLT,WLC:
                JvnServerImpl js1 = JvnServerImpl.jvnGetServer();
                object = js1.jvnLockWrite(id);
                state = lockState.WLT;
                break;
/*            case WLC:
                state = lockState.WLT;
                break;*/
            default:
                JvnServerImpl js = JvnServerImpl.jvnGetServer();
                object = js.jvnLockWrite(id);
                state = lockState.WLT;
                break;
        }
        System.out.println("Write lock taken");
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
}