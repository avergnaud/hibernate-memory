package com.poc.hibernate.model;

public class Data {
	
	private int id;
	private String text;
	
	public Data(){};
	
	public Data(String text){
		this.text = text;
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public int getId(){
		return this.id;
	}
	
	public void setText(String text){
		this.text = text;
	}
	
	public String getText(){
		return this.text;
	}
}