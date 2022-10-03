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
            case RLT, WLT, RLT_WLC:
                try {
                    while (state == lockState.RLT || state == lockState.WLT || state == lockState.RLT_WLC) {
                        wait();
                    }
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
            case RLT, WLT, RLT_WLC:
                try {
                    while (state == lockState.RLT || state == lockState.WLT || state == lockState.RLT_WLC) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    throw new JvnException("Interrupted while waiting for lock");
                }
                break;
            default:
                state = lockState.NL;
                break;
        }
        return object;
    }

    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        switch (state) {
            case WLT, RLT_WLC:
                try {
                    while (state == lockState.WLT || state == lockState.RLT_WLC) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    throw new JvnException("Interrupted while waiting for lock");
                }
                break;
            case RLT:
                break;
            default:
                state = lockState.RLT;
                break;
        }
        return object;
    }

    @Override
    public synchronized void jvnLockRead() throws JvnException {
        switch (state) {
            case RLC:
                state = lockState.RLT;
                break;
            case WLC:
                state = lockState.RLT_WLC;
                break;
            case WLT, RLT, RLT_WLC:
                throw new JvnException("Lock already taken");
            default:
                JvnServerImpl js = JvnServerImpl.jvnGetServer();
                Serializable jo = js.jvnLockRead(id);
                state = lockState.RLT;
                break;
        }
    }

    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        switch (state) {
            case WLT:
                throw new JvnException("Lock already taken");
            default:
                JvnServerImpl js = JvnServerImpl.jvnGetServer();
                Serializable jo = js.jvnLockWrite(id);
                state = lockState.WLT;
                break;
        }
    }

    @Override
    public synchronized void jvnUnlock() throws JvnException {
        switch (state) {
            case RLT:
                state = lockState.RLC;
                break;
            case WLT:
                state = lockState.WLC;
                break;
            case RLT_WLC:
                state = lockState.WLC;
                break;
            default:
        }
        notifyAll();
    }

    public void jvnSetObjectState(Serializable o) throws JvnException {
        object = o;
    }
}