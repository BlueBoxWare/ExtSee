class Turtles {

  public int fieldOnTurtles;

  public void methodOnTurtles() { }

}

class All extends Turtles implements Ninja {

  public int fieldOnAll;

  public void methodOnAll() { }

}

class The extends All implements Power {

  public int fieldOnThe;

  public void methodOnThe() { }

}

class Way extends The {

  public int fieldOnWay;

  public void methodOnWay() { }

}

class Down extends Way {

  public int fieldOnDown;

  public void methodOnDown() { }

}

interface Ninja {

  void methodOnNinja() { }

}

interface Power {

  void methodOnPower() { }

}