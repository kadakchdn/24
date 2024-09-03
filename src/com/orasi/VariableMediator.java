package com.orasi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableMediator {

  private final List<VariableWrapper> variableList = new ArrayList<>(10);

  public void clear() {
    variableList.clear();
  }
  
  public void addVariable(VariableWrapper vW) {
    this.variableList.add( vW );
  }
  
  public Map<String,String> generateVariableDefinitions() {
    Map<String,String> vM = new HashMap<>( variableList.size() );
    variableList.forEach( (t) -> {
      vM.put( t.getName(), t.getTextValue() );
    } );
    return vM;
  }
  
  public Map<String,Object> generateVariables() {
    Map<String,Object> vM = new HashMap<>( variableList.size() );
    variableList.forEach( (t) -> {
      vM.put( t.getName(), t.getValue() );
    } );
    return vM;
  }

}