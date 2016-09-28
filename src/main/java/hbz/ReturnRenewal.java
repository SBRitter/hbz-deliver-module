package hbz;

public class ReturnRenewal {

  private String patron;
  private String loan;

  public ReturnRenewal() {
    // Empty constructor needed for de-/encoding of JSON objects
  }

  public ReturnRenewal(String patron, String loan) {
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
