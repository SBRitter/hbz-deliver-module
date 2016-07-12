package hbz;

public class ItemReturn {

  private String patron;
  private String loan;

  public ItemReturn() {
    // Empty constructor needed for de-/encoding of JSON objects
  }

  public ItemReturn(String patron, String loan) {
    this.patron = patron;
    this.loan = loan;
  }

  public String getPatron() {
    return patron;
  }

  public void setPatron(String patron) {
    this.patron = patron;
  }

  public String getLoan() {
    return loan;
  }

  public void setLoan(String loan) {
    this.loan = loan;
  }

}
