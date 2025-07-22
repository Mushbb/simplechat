package com.example.jdbcsql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.List;
import java.util.ArrayList;

public class JDBC_SQL {
	 // 데이터베이스 연결 정보 설정 (실제 환경에 맞게 변경)
    private static final String DB_HOST = "localhost"; // 또는 SQL Server의 IP 주소
    private static final Integer DB_PORT = 1433; // SQL Server 기본 포트
    private static final String DB_NAME = "IT_Shopping_Mall"; // 연결할 데이터베이스 이름
    private static final String DB_USER = "mushbb";     // SQL Server 사용자 이름
    private static final String DB_PASSWORD = "mushbb"; // SQL Server 비밀번호

    public static Integer login(String id, String password, String connectionUrl) {
//        String connectionUrl = "jdbc:sqlserver://" + DB_HOST + ":" + DB_PORT +
//                ";databaseName=" + DB_NAME + ";user=" + DB_USER + ";password=" + DB_PASSWORD +
//                ";trustServerCertificate=true;"; // 개발/테스트 환경에서만 true, 운영은 CA 발급 인증서 사용

		Connection connection = null;
		Statement statement = null;
		ResultSet resultSet = null;
		Integer count = 0;
		
		try {
	        System.out.println("Connecting to SQL Server...");
	        connection = DriverManager.getConnection(connectionUrl);
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
    
    public static List<String> excuteQuery(String sqlQuery) {
    	List<String> result = new ArrayList<String>();
    	
        // JDBC URL 구성
        // 통합 보안 (Windows 인증) 사용 시:
//        String connectionUrl = "jdbc:sqlserver://" + DB_HOST + ":" + DB_PORT +
//                               ";databaseName=" + DB_NAME + ";integratedSecurity=true;trustServerCertificate=true;";
        // SQL Server 인증 사용 시:
        String connectionUrl = "jdbc:sqlserver://" + DB_HOST + ":" + DB_PORT +
                               ";databaseName=" + DB_NAME + ";user=" + DB_USER + ";password=" + DB_PASSWORD +
                               ";trustServerCertificate=true;"; // 개발/테스트 환경에서만 true, 운영은 CA 발급 인증서 사용

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        
        try {
            // 1. JDBC 드라이버 로드 (최신 JDBC 버전에서는 생략 가능하지만 명시적으로 적는 경우도 많음)
            // Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            // 2. 데이터베이스 연결 설정
            System.out.println("Connecting to SQL Server...");
            connection = DriverManager.getConnection(connectionUrl);
            System.out.println("Connection successful!");
            connection.setAutoCommit(false); 
            // 3. Statement 객체 생성 (SQL 쿼리 실행용)
            statement = connection.createStatement();

            if( sqlQuery.toLowerCase().startsWith("select ") ) {
	            resultSet = statement.executeQuery(sqlQuery);
	
	            // ResultSetMetaData 객체 가져오기
	            ResultSetMetaData rsmd = resultSet.getMetaData();
	            int columnCount = rsmd.getColumnCount(); // 총 컬럼 개수
	            String temp = "";
	            // 컬럼 헤더 출력
	            for (int i = 1; i <= columnCount; i++) {
	                temp += rsmd.getColumnName(i) + "\t"; // 컬럼 이름
	            }
	            result.add(temp);
	            temp = "";
	            
	            // 각 행의 모든 데이터 출력
	            while (resultSet.next()) {
	                for (int i = 1; i <= columnCount; i++) {
	                    temp += resultSet.getObject(i) + (i==columnCount?"":"\t"); // Object 타입으로 모든 데이터 가져오기
	                }
	                result.add(temp);
	                temp = "";
	            }
            } else {
            	System.out.println("Executing DML/DDL: " + sqlQuery);
            	int rowsAffected = statement.executeUpdate(sqlQuery);
            	System.out.println("Rows affected: " + rowsAffected);

            	connection.commit();
                System.out.println("Transaction committed successfully.");
                result.add("Rows affected: " + rowsAffected);
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
}
