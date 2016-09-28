package hbz;

public class Delivery {

  private String patron;
  private String item;

  public Delivery() {
    // Empty constructor needed for de-/encoding of JSON objects
  }

  public Delivery(String patron, String item) {
    this.patron = patron;
    this.item = item;
  }

  public String getPatron() {
    return patron;
  }

  public void setPatron(String patron) {
    this.patron = patron;
  }

  public String getItem() {
    return item;
  }

  public void setItem(String item) {
    this.item = item;
  }

}
