package com.example.simplechat.repository;

import com.example.simplechat.model.User;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 사용자(User) 엔티티의 영속성(데이터베이스 CRUD)을 관리하는 리포지토리 클래스입니다.
 * {@link JDBC_SQL}을 사용하여 데이터베이스와 상호작용합니다.
 */
@RequiredArgsConstructor
@Repository
public class UserRepository {
	private final JDBC_SQL jdbcsql;
	
	/**
	 * 사용자 이름으로 사용자를 조회합니다.
	 *
	 * @param username 조회할 사용자의 사용자 이름
	 * @return 해당 사용자 이름의 {@link User} 객체를 포함하는 {@link Optional}. 사용자가 없으면 Optional.empty() 반환.
	 */
	public Optional<User> findByUsername(String username) {
		String sql = "SELECT * FROM users WHERE username = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{username});

		if( parsedTable.isEmpty() )
			return Optional.empty();
		
		Map<String, Object> userRow = parsedTable.get(0);
		return Optional.of(mapRowToUser(userRow));
	}
	
	/**
	 * 사용자 ID로 사용자를 조회합니다.
	 *
	 * @param id 조회할 사용자의 ID
	 * @return 해당 ID의 {@link User} 객체를 포함하는 {@link Optional}. 사용자가 없으면 Optional.empty() 반환.
	 */
	public Optional<User> findById(Long id) {
		String sql = "SELECT * FROM users WHERE user_id = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{id});

		if( parsedTable.isEmpty() )
			return Optional.empty();
		
		Map<String, Object> userRow = parsedTable.get(0);
		return Optional.of(mapRowToUser(userRow));
	}
	
	/**
	 * 데이터베이스 행(Map)을 {@link User} 엔티티로 매핑합니다.
	 *
	 * @param row 데이터베이스에서 조회된 단일 행을 나타내는 Map
	 * @return 매핑된 {@link User} 객체
	 */
	public static User mapRowToUser(Map<String, Object> row) {
		User user = new User((Long)row.get("user_id") , (String) row.get("username"));

		user.setPassword_hash((String) row.get("password_hash"));
        user.setNickname((String) row.get("nickname"));
        user.setStatus_message((String) row.get("status_message"));
        user.setProfile_image_url((String) row.get("profile_image_url"));
		
		Object createdAtObj = row.get("created_at");
		if (createdAtObj instanceof Timestamp) {
			user.setCreated_at(((Timestamp) createdAtObj).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		} else if (createdAtObj != null) {
			user.setCreated_at(createdAtObj.toString());
		} else {
			user.setCreated_at(null);
		}
		
		return user;
	}
	
	/**
	 * 사용자를 저장합니다. 사용자 ID가 없으면 새로운 사용자를 삽입하고, ID가 있으면 기존 사용자를 업데이트합니다.
	 *
	 * @param user 저장할 {@link User} 객체
	 * @return 저장 또는 업데이트된 {@link User} 객체 (새 ID 및 생성 시간 포함)
	 */
	public User save(User user) {
		if( user.getId() == null ) {
			return insert(user);
		} else
			return update(user);
	}
	
	/**
	 * 새로운 사용자를 데이터베이스에 삽입합니다.
	 * 생성된 사용자 ID와 생성 시간을 {@link User} 객체에 채웁니다.
	 *
	 * @param user 삽입할 {@link User} 객체
	 * @return ID와 생성 시간이 채워진 {@link User} 객체
	 */
	private User insert(User user) {
		String sql = "INSERT INTO users (username, password_hash, nickname) VALUES ( ?, ?, ? )";
		Map<String, Object> result = jdbcsql.executeUpdate(sql, 
				new Object[]{user.getUsername(), user.getPassword_hash(), user.getNickname()},
				new String[] {"user_id"}, new String[] {"created_at"});
		
		if( result != null && result.containsKey("user_id")) {
			user.setId(((Number)result.get("user_id")).longValue());
			user.setCreated_at(((Timestamp)result.get("created_at")).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		}
		return user;
	}
	
	/**
	 * 기존 사용자 정보를 업데이트합니다.
	 * {@link User#getChangedFields(User)}를 사용하여 변경된 필드만 동적으로 SQL 쿼리를 구성하여 업데이트합니다.
	 *
	 * @param user 업데이트할 {@link User} 객체
	 * @return 업데이트된 {@link User} 객체
	 * @throws RuntimeException 사용자를 찾을 수 없거나 업데이트에 실패한 경우
	 */
	private User update(User user) {
        Optional<User> fromdb = findById(user.getId());
		if (fromdb.isEmpty())
			throw new RuntimeException("사용자 ID로 사용자를 찾을 수 없습니다: " + user.getId());
		
		Map<String, Object> Changed = user.getChangedFields(fromdb.get());
		if (Changed.isEmpty()) { return user; } // 변경 사항 없으면 바로 리턴
		
		StringBuilder sql = new StringBuilder("UPDATE users SET ");
		List<Object> values = new ArrayList<>();
		
		for( Map.Entry<String, Object> entry : Changed.entrySet() ) {
			sql.append(entry.getKey()).append(" = ?, ");
			values.add(entry.getValue());
		}
		sql.delete(sql.length() - 2, sql.length()); // 마지막 ", " 제거
		sql.append(" WHERE user_id = ?");
		values.add(user.getId());
		
		Long affectedRows = (long)jdbcsql.executeUpdate(sql.toString(), values.toArray(), null, null).get("affected_rows");
		
		if (affectedRows == null || affectedRows == 0L) {
			throw new RuntimeException("ID " + user.getId() + "를 가진 사용자를 찾을 수 없거나 업데이트할 수 없습니다.");
		}
		
		return user;
	}
	
	/**
	 * 미리 정의된 ID를 사용하여 새로운 사용자를 삽입합니다.
	 * `JDBC_SQL.executeInsert_IdentitiyOn`을 사용합니다.
	 *
	 * @param user 삽입할 {@link User} 객체 (ID 포함)
	 * @return 삽입된 {@link User} 객체
	 */
	public User insertwithId(User user) {
		String sql = "INSERT INTO users (user_id, username, password_hash, nickname) VALUES ( ?, ?, ?, ? )";
		jdbcsql.executeInsert_IdentitiyOn(sql, 
				new Object[]{user.getId(), user.getUsername(), user.getPassword_hash(), user.getNickname()});

		return user;
	}
	
	/**
	 * 사용자 ID를 기준으로 사용자를 삭제합니다.
	 *
	 * @param id 삭제할 사용자의 ID
	 * @throws RuntimeException 사용자를 찾을 수 없거나 삭제에 실패한 경우
	 */
	public void deleteById(Long id) {
		String sql = "DELETE FROM users WHERE user_id = ?";
		Long affectedRows = (long)jdbcsql.executeUpdate(sql, new Object[]{id}, null, null).get("affected_rows");
		
		if (affectedRows == null || affectedRows == 0L) {
			throw new RuntimeException("ID " + id + "를 가진 사용자를 찾을 수 없거나 삭제할 수 없습니다.");
		}
	}
	
	/**
	 * 특정 사용자 이름의 사용자가 존재하는지 확인합니다.
	 *
	 * @param username 확인할 사용자의 사용자 이름
	 * @return 해당 사용자 이름의 사용자가 존재하면 true, 그렇지 않으면 false
	 */
	public boolean existsByUsername(String username) {
		String sql = "SELECT COUNT(1) FROM users WHERE username = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{username});

		if (parsedTable.isEmpty()) {
			return false;
		}

		long count = ((Number) parsedTable.get(0).values().iterator().next()).longValue();
		return count > 0;
	}
	
	/**
	 * 특정 ID의 사용자가 존재하는지 확인합니다.
	 *
	 * @param userId 확인할 사용자의 ID
	 * @return 해당 ID의 사용자가 존재하면 true, 그렇지 않으면 false
	 */
	public boolean existsById(Long userId) {
		String sql = "SELECT COUNT(1) FROM users WHERE user_id = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{userId});

		if (parsedTable.isEmpty()) {
			return false;
		}

		long count = ((Number) parsedTable.get(0).values().iterator().next()).longValue();
		return count > 0;
	}
	
	/**
	 * 모든 사용자 목록을 조회합니다.
	 *
	 * @return 모든 {@link User} 객체 목록. 사용자가 없으면 빈 리스트를 반환합니다.
	 */
	public List<User> findAll() {
		String sql = "SELECT * FROM users";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, null);

		if (parsedTable.isEmpty()) {
			return Collections.emptyList();
		}

		return parsedTable.stream()
			.map(UserRepository::mapRowToUser)
			.collect(Collectors.toList());
	}
	
	/**
	 * 전체 사용자 수를 반환합니다.
	 *
	 * @return 전체 사용자 수
	 */
	public long count() {
		String sql = "SELECT COUNT(*) FROM users";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, null);

		if (parsedTable.isEmpty()) {
			return 0L;
		}
		
		return ((Number) parsedTable.get(0).values().iterator().next()).longValue();
	}
	
	/**
	 * 사용자 ID를 기준으로 특정 프로필 관련 필드를 조회합니다.
	 *
	 * @param userId 프로필 정보를 조회할 사용자의 ID
	 * @return 사용자 프로필 정보를 담은 {@link Map}을 포함하는 {@link Optional}. 사용자가 없으면 Optional.empty() 반환.
	 */
	public Optional<Map<String, Object>> findProfileById(Long userId) {
	    String sql = "SELECT user_id, username, nickname, status_message, profile_image_url FROM users WHERE user_id = ?";
	    List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{userId});

	    if (parsedTable.isEmpty()) {
	        return Optional.empty();
	    }

	    return Optional.of(parsedTable.get(0));
	}

}
