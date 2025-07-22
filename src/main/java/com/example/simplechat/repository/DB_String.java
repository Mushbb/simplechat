package com.example.simplechat.repository;

import java.util.Map;

public class DB_String {
	private final String DB_HOST;
	private final String DB_PORT;
	private final String DB_NAME;
	private final String DB_USER;
	private final String DB_PASSWORD;
	
	private static DB_String INSTANCE;
	
	private DB_String(String DB_HOST, String DB_PORT, String DB_NAME, String DB_USER, String DB_PASSWORD){
		this.DB_HOST = DB_HOST;
		this.DB_PORT = DB_PORT;
		this.DB_NAME = DB_NAME;
		this.DB_USER = DB_USER;
		this.DB_PASSWORD = DB_PASSWORD;
	}
	
	public static DB_String getInstance() {
        return INSTANCE;
    }
	
	public static DB_String configure(Map<String, String> config){
		if( INSTANCE == null ) {
			INSTANCE = new DB_String(config.get("DB_HOST"), config.get("DB_PORT"), config.get("DB_NAME"), config.get("DB_USER"), config.get("DB_PASSWORD"));
		}
		
		return INSTANCE;
	}
	
	public String connectionUrl() {
		return "jdbc:sqlserver://" + DB_HOST + ":" + DB_PORT +
                ";databaseName=" + DB_NAME + ";user=" + DB_USER + ";password=" + DB_PASSWORD +
                ";trustServerCertificate=true;";
	}
}