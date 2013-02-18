package org.jboss.arquillian.container.android.api;

public interface ObjectStore {
    <T> ObjectStore add(Class<T> type, T instance);
    
    <T> T get(Class<T> type);
    
    ObjectStore clear();
    
    int size();

    boolean isEmpty();
}
