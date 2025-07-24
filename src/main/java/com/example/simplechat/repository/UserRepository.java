package com.example.simplechat.repository;

import com.example.simplechat.model.User;
import com.example.sql.JDBC_SQL;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.stream.Collectors;
import java.sql.Timestamp;

public class UserRepository {
	public Optional<User> findByUsername(String Username) {
		String sql = "SELECT * FROM users WHERE username = ?";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, new String[]{Username});
		
		if( parsedTable.isEmpty() )
			return Optional.empty();
		
		Map<String, Object> userRow = parsedTable.get(0);
		return Optional.of(mapRowToUser(userRow));
	}
	
	public Optional<User> findById(Long Id) {
		String sql = "SELECT * FROM users WHERE user_id = ?";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, new String[]{""+Id});
		
		if( parsedTable.isEmpty() )
			return Optional.empty();
		
		Map<String, Object> userRow = parsedTable.get(0);
		return Optional.of(mapRowToUser(userRow));
	}
	
	public static User mapRowToUser(Map<String, Object> row) {
		User user = new User((Long) row.get("user_id"), (String) row.get("username"));
		user.setNickname((String) row.get("nickname"));
		user.setCreated_at(((Timestamp)row.get("created_at")).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		
		return user;
	}
	
	public User save(User user) {
		if( user.getId() == null ) {
			return insert(user);
		} else
			return update(user);
	}
	
	private User insert(User user) {
		// db에 insert하고 id를 받아와 객체에 채움
		String sql = "INSERT INTO users (username, password_hash, nickname) VALUES ( ?, ?, ? )";
		Map<String, Object> result = JDBC_SQL.executeUpdate(sql, 
				new String[]{user.getUsername(), user.getPassword_hash(), user.getNickname()},
				new String[] {"user_id", "created_at"});
		
		if( result != null ) {
			user.setId((long)result.get("user_id"));
			user.setCreated_at(((Timestamp)result.get("created_at")).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		}
		return user;
	}
	
	private User update(User user) {
		// db에서 기존값을 가져와서 비교
		Optional<User> fromdb = findByUsername(user.getUsername());
		if (fromdb.isEmpty())
			throw new RuntimeException("User not found with username: " + user.getUsername());
		Map<String, Object> Changed = user.getChangedFields(fromdb.get());
		if (Changed.isEmpty()) { return user; } // 변경 사항 없으면 바로 리턴
		
		String sql = "UPDATE users SET ";
		String[] values = new String[Changed.size()+1];
		int i = 0;
		for( String key : Changed.keySet() ) {
			sql += key + " = ? , ";
			values[i++] = (String)Changed.get(key);
		}
		sql = sql.substring(0, -2);
		sql += "WHERE id = ?";
		values[i] = ""+user.getId();
		
		// 변경된 필드만 갱신
		Long affectedRows = (long)JDBC_SQL.executeUpdate(sql, values, null).get("affected_rows");
		
		if (affectedRows == null || affectedRows == 0L) {
			throw new RuntimeException("User with ID " + user.getId() + " not found or could not be deleted.");
		}
		
		return user;
	}
	
	public void deleteById(Long Id) {
		String sql = "DELETE FROM users WHERE user_id = ?";
		Long affectedRows = (long)JDBC_SQL.executeUpdate(sql, new String[]{""+Id}, null).get("affected_rows");
		
		if (affectedRows == null || affectedRows == 0L) {
			throw new RuntimeException("User with ID " + Id + " not found or could not be deleted.");
		}
	}
	
	public boolean existsByUsername(String username) {
		// 전체 컬럼을 가져올 필요 없이, 존재 여부만 확인하면 되므로 COUNT(1)이 효율적입니다.
		String sql = "SELECT COUNT(1) FROM users WHERE username = ?";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, new String[]{username});

		if (parsedTable.isEmpty()) {
			return false;
		}

		// COUNT 결과는 보통 Long 타입으로 반환됩니다.
		// DB Utils나 JDBC 드라이버에 따라 키 이름이 다를 수 있습니다. (예: "COUNT(1)")
		// 첫 번째 행의 첫 번째 값을 가져옵니다.
		long count = (long) parsedTable.get(0).values().iterator().next();
		return count > 0;
	}
	
	public List<User> findAll() {
		String sql = "SELECT * FROM users";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, null); // 파라미터 없음

		if (parsedTable.isEmpty()) {
			return Collections.emptyList();
		}

		// 각 Map을 User 객체로 변환하여 리스트로 만듭니다.
		return parsedTable.stream()
			.map(UserRepository::mapRowToUser)
			.collect(Collectors.toList());
	}
	
	public long count() {
		String sql = "SELECT COUNT(*) FROM users";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, null);

		if (parsedTable.isEmpty()) {
			return 0L;
		}
		
		// existsByUsername과 마찬가지로, 첫 번째 행의 첫 번째 값을 Long으로 변환합니다.
		return (long) parsedTable.get(0).values().iterator().next();
	}
}
