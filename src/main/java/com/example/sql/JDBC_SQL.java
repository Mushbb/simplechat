package com.example.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Statement;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.example.simplechat.repository.DB_String;
import com.example.simplechat.repository.DB_Utils;

import java.util.ArrayList;

public class JDBC_SQL {
    public static Integer login(String id, String password) {
		Connection connection = null;
		Statement statement = null;
		ResultSet resultSet = null;
		Integer count = 0;
		
		try {
	        System.out.println("Connecting to SQL Server...");
	        connection = DriverManager.getConnection(DB_String.getInstance().connectionUrl());
	        System.out.println("Connection successful!");
	        
	        statement = connection.createStatement();
	        
	        resultSet = statement.executeQuery("SELECT COUNT(*) AS CNT FROM SM_Mem_Info WHERE MI_ID='"+id+"' AND MI_PW='"+password+"'");
	        resultSet.next();
	        count = resultSet.getInt("CNT");
	        
		} catch (SQLException e) {
            System.err.println("Database error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 6. 자원 해제 (역순으로 닫는 것이 좋음)
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
                System.out.println("\nDatabase connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
		
		return count;
	}
    
    public static List<Map<String, Object>> executeSelect(String sqlQuery, String[] Params) {
    	List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            // 1. JDBC 드라이버 로드 (최신 JDBC 버전에서는 생략 가능하지만 명시적으로 적는 경우도 많음)
            // Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            // 2. 데이터베이스 연결 설정
            System.out.println("Connecting to SQL Server...");
            connection = DriverManager.getConnection(DB_String.getInstance().connectionUrl());
            System.out.println("Connection successful!");
            // 3. Statement 객체 생성 (SQL 쿼리 실행용) + 파라미터 등록
            statement = connection.prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS);
            if( Params != null )
	            for(int i=0;i<Params.length;i++)
	            	statement.setObject(i+1, Params[i]);

            resultSet = statement.executeQuery();

            // ResultSetMetaData 객체 가져오기
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int columnCount = rsmd.getColumnCount(); // 총 컬럼 개수
            
            // 각 행의 모든 데이터 출력
            while (resultSet.next()) {
            	Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                	// 컬럼 이름을 키로, 실제 타입의 객체를 값으로 저장
                	row.put(rsmd.getColumnName(i).toLowerCase(), resultSet.getObject(i));
                }
                result.add(row);
            }
        } catch (SQLException e) {
        	System.err.println("Database error occurred during DML/DDL operation: " + e.getMessage());
            e.printStackTrace();
            try {
                if (connection != null) {
                    connection.rollback();
                    System.err.println("Transaction rolled back due to error.");
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Error during rollback: " + rollbackEx.getMessage());
            }
        } finally {
            // 6. 자원 해제 (역순으로 닫는 것이 좋음)
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
                System.out.println("\nDatabase connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        
        return result;
    }
    
    public static Map<String, Object> executeUpdate(String sqlQuery, String[] Params, String[] returnCols, String[] addCols) {
    	Map<String, Object> result = new HashMap<>();
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            // 1. JDBC 드라이버 로드 (최신 JDBC 버전에서는 생략 가능하지만 명시적으로 적는 경우도 많음)
            // Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            // 2. 데이터베이스 연결 설정
            System.out.println("Connecting to SQL Server...");
            connection = DriverManager.getConnection(DB_String.getInstance().connectionUrl());
            System.out.println("Connection successful!");
            connection.setAutoCommit(false); 
            
            // 3. Statement 객체 생성 (SQL 쿼리 실행용) + 파라미터 등록
            System.out.println("statement");
            statement = connection.prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS);
            
            if( Params != null )
	            for(int i=0;i<Params.length;i++)
	            	statement.setObject(i+1, Params[i]);

        	System.out.println("Executing DML/DDL: " + sqlQuery);
        	int rowsAffected = statement.executeUpdate();
        	System.out.println("Rows affected: " + rowsAffected);
        	
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if ( generatedKeys.next() )
            	for(int i=0;i<returnCols.length;i++)
            		result.put(returnCols[i], generatedKeys.getObject(i+1));
            
            statement.close();
            
            String SelectSql = "SELECT ";
            for( String Cols : addCols )
            	SelectSql += Cols+", ";
            SelectSql = SelectSql.substring(0, SelectSql.length()-2);
            SelectSql += " FROM "+DB_Utils.TableNameFromInsert(sqlQuery)+" ";
            SelectSql += "WHERE "+returnCols[0]+" = "+result.get(returnCols[0]);
            System.out.println(SelectSql);
            
            statement = connection.prepareStatement(SelectSql);
            ResultSet resultSet = statement.executeQuery();
            // ResultSetMetaData 객체 가져오기
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int columnCount = rsmd.getColumnCount(); // 총 컬럼 개수
            
            // 각 행의 모든 데이터 출력
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                	// 컬럼 이름을 키로, 실제 타입의 객체를 값으로 저장
                	result.put(rsmd.getColumnName(i).toLowerCase(), resultSet.getObject(i));
                }
            }
            
            result.put("affected_rows", (long) rowsAffected);
            generatedKeys.close();
            
            connection.commit();
            System.out.println("Transaction committed successfully.");
            
        } catch (SQLException e) {
        	System.err.println("Database error occurred during DML/DDL operation: " + e.getMessage());
            e.printStackTrace();
            try {
                if (connection != null) {
                    connection.rollback();
                    System.err.println("Transaction rolled back due to error.");
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Error during rollback: " + rollbackEx.getMessage());
            }
        } finally {
            // 6. 자원 해제 (역순으로 닫는 것이 좋음)
            try {
                if (statement != null) statement.close();
                if (connection != null) connection.close();
                System.out.println("\nDatabase connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        
        return result;
    }
    
    public static Map<String, Object> executeInsert_IdentitiyOn(String sqlQuery, String[] Params) {
    	Map<String, Object> result = new HashMap<>();
        Connection connection = null;
        PreparedStatement statement = null;
        Statement identity = null;
        
        try {
            // 1. JDBC 드라이버 로드 (최신 JDBC 버전에서는 생략 가능하지만 명시적으로 적는 경우도 많음)
            // Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            // 2. 데이터베이스 연결 설정
            System.out.println("Connecting to SQL Server...");
            connection = DriverManager.getConnection(DB_String.getInstance().connectionUrl());
            System.out.println("Connection successful!");
            connection.setAutoCommit(false); 
            
            identity = connection.createStatement();
            identity.execute("SET IDENTITY_INSERT users ON" );
            
            // 3. Statement 객체 생성 (SQL 쿼리 실행용) + 파라미터 등록
            statement = connection.prepareStatement(sqlQuery);
            if( Params != null )
	            for(int i=0;i<Params.length;i++)
	            	statement.setObject(i+1, Params[i]);
            statement.executeUpdate();
            
        	System.out.println("Executing DML/DDL: " + sqlQuery);
        	System.out.println(Params[0]+" "+Params[1]+" "+Params[2]+" "+Params[3]);
            
            identity.execute("SET IDENTITY_INSERT users OFF" );
            
            connection.commit();
            System.out.println("Transaction committed successfully.");
            
        } catch (SQLException e) {
        	System.err.println("Database error occurred during DML/DDL operation: " + e.getMessage());
            e.printStackTrace();
            try {
                if (connection != null) {
                    connection.rollback();
                    System.err.println("Transaction rolled back due to error.");
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Error during rollback: " + rollbackEx.getMessage());
            }
        } finally {
            // 6. 자원 해제 (역순으로 닫는 것이 좋음)
            try {
            	if (identity != null) identity.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
                System.out.println("\nDatabase connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        
        return result;
    }
}
