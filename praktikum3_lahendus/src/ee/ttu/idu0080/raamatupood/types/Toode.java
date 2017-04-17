package ee.ttu.idu0080.raamatupood.types;

import java.io.Serializable;
import java.math.BigDecimal;

public class Toode implements Serializable {
	private Integer kood;
	private String nimetus;
	private BigDecimal hind;

	public Toode(int kood, String nimetus, BigDecimal hind) {
		this.kood = kood;
		this.nimetus = nimetus;
		this.hind = hind;
	}
	
	public Integer getKood() {
		return kood;
	}
	
	public void setKood(Integer kood) {
		this.kood = kood;
	}
	
	public BigDecimal getHind() {
		return hind;
	}

	public void setHind(BigDecimal hind) {
		this.hind = hind;
	}
	
	@Override
	public String toString() {
		return "Toode: kood=" + kood + ", nimetus=" + nimetus + ", hind=" + hind;
	}
}
