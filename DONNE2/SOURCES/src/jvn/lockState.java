package jvn;

public enum lockState {
    NL,// no local lock
    RLC, // read lock cached
    WLC, // write lock cached
    RLT, // read lock taken
    WLT, // write lock taken
    RLT_WLC, // read lock taken – write lock cached
}
