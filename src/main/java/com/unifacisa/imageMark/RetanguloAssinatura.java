package com.unifacisa.imageMark;

public class RetanguloAssinatura {

	private int id;
	private double pontoInicialX;
	private double pontoInicialY;
	private double largura;
	private double altura;

	public double getPontoInicialX() {
		return pontoInicialX;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setPontoInicialX(double pontoInicialX) {
		this.pontoInicialX = pontoInicialX;
	}

	public double getPontoInicialY() {
		return pontoInicialY;
	}

	public void setPontoInicialY(double pontoInicialY) {
		this.pontoInicialY = pontoInicialY;
	}

	public double getLargura() {
		return largura;
	}

	public void setLargura(double largura) {
		this.largura = largura;
	}

	public double getAltura() {
		return altura;
	}

	public void setAltura(double altura) {
		this.altura = altura;
	}

}
