package com.orasi;

public class FunctionVariableWrapper {

  private final String name;
  private final int type;

  public FunctionVariableWrapper(String name, int type) {
    this.name = name;
    this.type = type;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the type
   */
  public int getType() {
    return type;
  }

  

}
