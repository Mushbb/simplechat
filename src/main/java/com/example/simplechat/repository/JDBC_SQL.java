package com.example.simplechat.repository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC를 직접 사용하여 데이터베이스와 상호작용하는 유틸리티 클래스입니다.
 * SQL SELECT, INSERT, UPDATE, DELETE 쿼리를 실행하는 일반적인 메서드를 제공합니다.
 * 트랜잭션 관리와 리소스 정리를 포함합니다.
 */
@Component
@RequiredArgsConstructor
public class JDBC_SQL {
    private static final Logger logger = LoggerFactory.getLogger(JDBC_SQL.class);
    private final DataSource dataSource;

    /**
     * 데이터베이스 연결을 가져옵니다.
     * @return 데이터베이스 연결
     * @throws SQLException 데이터베이스 접근 오류 발생 시
     */
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * SQL SELECT 쿼리를 실행하고 결과를 List<Map<String, Object>> 형태로 반환합니다.
     * PreparedStatement를 사용하여 SQL 인젝션을 방지합니다.
     *
     * @param sqlQuery 실행할 SELECT 쿼리 문자열
     * @param params 쿼리의 Placeholder(?)에 바인딩될 매개변수 배열
     * @return 쿼리 결과의 각 행을 Map으로 표현한 리스트. 컬럼 이름은 키로 사용됩니다.
     */
    public List<Map<String, Object>> executeSelect(String sqlQuery, Object[] params) {
    	List<Map<String, Object>> result = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = connection.prepareStatement(sqlQuery);
            if (params != null) {
	            for (int i = 0; i < params.length; i++) {
	            	statement.setObject(i + 1, params[i]);
	            }
            }

            resultSet = statement.executeQuery();

            ResultSetMetaData rsmd = resultSet.getMetaData();
            int columnCount = rsmd.getColumnCount();
            
            while (resultSet.next()) {
            	Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                	row.put(rsmd.getColumnName(i).toLowerCase(), resultSet.getObject(i));
                }
                result.add(row);
            }
        } catch (SQLException e) {
        	logger.error("SELECT 작업 중 데이터베이스 오류 발생: {}", e.getMessage(), e);
            try {
                if (connection != null) {
                    connection.rollback();
                    logger.warn("오류로 인해 트랜잭션이 롤백되었습니다.");
                }
            } catch (SQLException rollbackEx) {
                logger.error("롤백 중 오류 발생: {}", rollbackEx.getMessage(), rollbackEx);
            }
        } finally {
            closeResources(resultSet, statement, connection);
        }
        
        return result;
    }
    
    /**
     * SQL INSERT, UPDATE, DELETE 쿼리를 실행합니다.
     * 생성된 키를 반환하거나 추가 컬럼을 조회하여 Map 형태로 반환할 수 있습니다.
     * 트랜잭션을 자동으로 커밋하거나 롤백합니다.
     *
     * @param sqlQuery 실행할 SQL 쿼리 문자열
     * @param params 쿼리의 Placeholder(?)에 바인딩될 매개변수 배열
     * @param returnCols 생성된 키를 반환받을 컬럼 이름 배열 (예: {"id"})
     * @param addCols 삽입 후 추가로 조회할 컬럼 이름 배열 (예: {"created_at"})
     * @return 영향을 받은 행의 수, 생성된 키 및 추가 조회 컬럼을 포함하는 Map
     */
    public Map<String, Object> executeUpdate(String sqlQuery, Object[] params, String[] returnCols, String[] addCols) {
    	Map<String, Object> result = new HashMap<>();
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            connection.setAutoCommit(false); 
            
            statement = connection.prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS);
            
            if (params != null) {
	            for (int i = 0; i < params.length; i++) {
	            	statement.setObject(i + 1, params[i]);
	            }
            }

        	int rowsAffected = statement.executeUpdate();
        	
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next() && returnCols != null) {
            	for (int i = 0; i < returnCols.length; i++) {
            		result.put(returnCols[i].toLowerCase(), generatedKeys.getObject(i + 1));
            	}
            }
            
            if (addCols != null && returnCols != null && returnCols.length > 0) {
            	generatedKeys.close(); // GeneratedKeys는 더 이상 필요 없으므로 닫음
            	statement.close(); // 기존 statement 닫고
	            String selectSql = "SELECT ";
	            for (String col : addCols) {
	            	selectSql += col + ", ";
	            }
	            selectSql = selectSql.substring(0, selectSql.length() - 2); // 마지막 ", " 제거
	            selectSql += " FROM " + DB_Utils.TableNameFromInsert(sqlQuery) + " ";
	            selectSql += "WHERE " + returnCols[0] + " = ?";
	            
	            statement = connection.prepareStatement(selectSql);
	            statement.setObject(1, result.get(returnCols[0].toLowerCase()));
	            
	            ResultSet resultSet = statement.executeQuery();
	            ResultSetMetaData rsmd = resultSet.getMetaData();
	            int columnCount = rsmd.getColumnCount();
	            
	            while (resultSet.next()) {
	                for (int i = 1; i <= columnCount; i++) {
	                	result.put(rsmd.getColumnName(i).toLowerCase(), resultSet.getObject(i));
	                }
	            }
	            resultSet.close();
            }
            
            result.put("affected_rows", (long) rowsAffected);
            
            connection.commit();
            
        } catch (SQLException e) {
        	logger.error("UPDATE/INSERT/DELETE 작업 중 데이터베이스 오류 발생: {}", e.getMessage(), e);
            try {
                if (connection != null) {
                    connection.rollback();
                    logger.warn("오류로 인해 트랜잭션이 롤백되었습니다.");
                }
            } catch (SQLException rollbackEx) {
                logger.error("롤백 중 오류 발생: {}", rollbackEx.getMessage(), rollbackEx);
            }
        } finally {
            closeResources(null, statement, connection);
        }
        
        return result;
    }
    
    /**
     * IDENTITY_INSERT를 ON으로 설정하여 ID를 수동으로 지정하는 INSERT 쿼리를 실행합니다.
     * 특정 레거시 시스템 또는 데이터 마이그레이션 시 사용될 수 있습니다.
     * <p>참고: `params`는 `Object[]`로 받는 것이 타입 안전성 측면에서 더 좋습니다.</p>
     *
     * @param sqlQuery 실행할 INSERT 쿼리 문자열
     * @param params 쿼리의 Placeholder(?)에 바인딩될 매개변수 배열
     * @return 작업 결과 (현재는 빈 Map)
     */
    public Map<String, Object> executeInsert_IdentitiyOn(String sqlQuery, Object[] params) {
    	Map<String, Object> result = new HashMap<>();
        Connection connection = null;
        PreparedStatement statement = null;
        Statement identityStatement = null;
        
        try {
            connection = getConnection();
            connection.setAutoCommit(false); 
            
            identityStatement = connection.createStatement();
            identityStatement.execute("SET IDENTITY_INSERT users ON"); // 테이블 이름이 하드코딩됨, 유의
            
            statement = connection.prepareStatement(sqlQuery);
            if (params != null) {
	            for (int i = 0; i < params.length; i++) {
	            	statement.setObject(i + 1, params[i]);
	            }
            }
            statement.executeUpdate();
            
            identityStatement.execute("SET IDENTITY_INSERT users OFF");
            
            connection.commit();
            
        } catch (SQLException e) {
        	logger.error("IDENTITY_INSERT 작업 중 데이터베이스 오류 발생: {}", e.getMessage(), e);
            try {
                if (connection != null) {
                    connection.rollback();
                    logger.warn("오류로 인해 트랜잭션이 롤백되었습니다.");
                }
            } catch (SQLException rollbackEx) {
                logger.error("롤백 중 오류 발생: {}", rollbackEx.getMessage(), rollbackEx);
            }
        } finally {
            closeResources(null, statement, connection);
            closeResources(null, identityStatement, null); // identityStatement만 닫음
        }
        
        return result;
    }
    
    /**
     * 하드코딩된 테이블 (`SM_Mem_Info`)을 사용하여 사용자 로그인을 처리합니다.
     * <p>
     * <b>경고: 이 메서드는 SQL 인젝션에 취약했습니다.</b>
     * 현재는 {@link PreparedStatement}를 사용하여 매개변수를 안전하게 바인딩하도록 수정되었습니다.
     * 그러나 `SM_Mem_Info`, `MI_ID`, `MI_PW`와 같은 테이블 및 컬럼 이름이 하드코딩되어 있어
     * 유연성이 떨어지고, {@link com.example.simplechat.service.AuthService}를 통해
     * 사용자 인증을 처리하는 것이 더 좋은 설계입니다.
     * </p>
     *
     * @param id 로그인할 사용자 ID
     * @param password 사용자 비밀번호
     * @return 일치하는 사용자 수가 0 또는 1 (성공 시 1)
     */
    public Integer login(String id, String password) {
		Connection connection = null;
		PreparedStatement statement = null; // PreparedStatement 사용
		ResultSet resultSet = null;
		Integer count = 0;
		
		try {
	        connection = getConnection();
	        // SQL 인젝션 방지를 위해 PreparedStatement 사용
	        String sql = "SELECT COUNT(*) AS CNT FROM SM_Mem_Info WHERE MI_ID = ? AND MI_PW = ?";
	        statement = connection.prepareStatement(sql);
	        statement.setString(1, id);
	        statement.setString(2, password);

	        resultSet = statement.executeQuery();
	        if (resultSet.next()) {
	            count = resultSet.getInt("CNT");
	        }
		} catch (SQLException e) {
            logger.error("데이터베이스 로그인 오류 발생: {}", e.getMessage(), e);
        } finally {
            closeResources(resultSet, statement, connection);
        }
		
		return count;
	}

    /**
     * JDBC 리소스(ResultSet, Statement, Connection)를 안전하게 닫습니다.
     * @param resultSet 닫을 ResultSet
     * @param statement 닫을 Statement
     * @param connection 닫을 Connection
     */
    private void closeResources(ResultSet resultSet, Statement statement, Connection connection) {
        try {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        } catch (SQLException e) {
            logger.error("JDBC 리소스 닫기 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}



