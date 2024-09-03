package com.orasi;

import com.orasi.alchemy.mediation.execution.StackableContext;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author allen
 */
public class ContextMap implements Map<String, Object>, StackableContext {

  private final Deque<Map<String, Object>> contextQueue = new ArrayDeque<>(5);
  private Map<String, Object> contextMap;
  private final Map<String, Object> globalMap = new HashMap<>(10);

  public ContextMap() {
    contextMap = new HashMap<>(10);
  }

  @Override
  public void promote(String key) {
    if ( key == null || key.trim().isEmpty() ) {
      return;
    }
    
    String[] splitKey = key.split( "=" );
    
    String keyName = key;
    String newName = key;
    
    if ( splitKey.length > 1 ) {
      keyName = splitKey[ 0 ];
      newName = splitKey[ 1 ];
    }

    
    if (contextMap.containsKey(keyName) && !contextQueue.isEmpty()) {
      contextQueue.peekFirst().put(newName, contextMap.get(keyName) );
    }
  }

  @Override
  public Object remove(String key, boolean global) {
    if (global) {
      return globalMap.remove(key);
    } else {
      return remove(key);
    }
  }

  @Override
  public Object put(String key, Object value, boolean global) {
    if (global) {
      return globalMap.put(key, value);
    } else {
      return put(key, value);
    }
  }	

  public synchronized void reset() {
    
    //
    // Remove listeners from the current context if available
    //
    if (contextMap != null) {
      contextMap.clear();
    }
    
    contextQueue.forEach((t) -> {
      t.clear();
    });
    globalMap.clear();
    contextQueue.clear();
    contextMap = new HashMap<>(10);
  }

  @Override
  public synchronized void push() {
    contextQueue.addFirst(contextMap);
    contextMap = new HashMap<>(10);
  }

  @Override
  public synchronized void pop() {
    if (contextMap != null) {
      contextMap.clear();
    }
    contextMap = contextQueue.removeFirst();
  }

  @Override
  public int size() {
    int size = contextMap.size();
    size += globalMap.size();

    return size;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    return contextMap.containsKey(key) || globalMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {

    return contextMap.containsValue(value) || globalMap.containsValue(value);
  }

  @Override
  public Object get(Object key) {

    if (contextMap.containsKey(key + "")) {
      return contextMap.get(key + "");
    }

    if (globalMap.containsKey(key + "")) {
      return globalMap.get(key + "");
    }

    return null;
  }

  @Override
  public Object put(String key, Object value) {
    return contextMap.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    return contextMap.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends Object> m) {
    contextMap.putAll(m);
  }

  @Override
  public void clear() {
    contextMap.clear();
  }

  @Override
  public Set<String> keySet() {
    Set<String> keySet = contextMap.keySet();
    keySet.addAll(globalMap.keySet());
    return keySet;
  }

  @Override
  public Collection<Object> values() {
    Collection<Object> values = contextMap.values();
    values.addAll(globalMap.values());
    return values;
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    Set<Entry<String, Object>> entrySet = contextMap.entrySet();
    entrySet.addAll(globalMap.entrySet());
    return entrySet;
  }

}
