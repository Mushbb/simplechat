package com.example.simplechat.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 데이터베이스 관련 유틸리티 함수를 제공하는 클래스입니다.
 * 주로 JDBC 쿼리 결과를 파싱하거나 SQL 쿼리 문자열에서 특정 정보를 추출하는 데 사용됩니다.
 */
public class DB_Utils {
	/**
	 * {@link JDBC_SQL}의 결과(List&lt;String&gt;)를 범용적인 List&lt;Map&lt;String, Object&gt;&gt; 형태로 파싱합니다.
	 * 첫 번째 문자열은 헤더(컬럼 이름)로 간주하며, 그 이후의 문자열은 탭으로 구분된 실제 데이터 행으로 간주합니다.
	 *
	 * @param rawResult {@link JDBC_SQL#excuteQuery(String)}로부터 반환된 원시 결과
	 * @return 파싱된 데이터. 각 Map은 하나의 행(row)을 나타내며, 키는 소문자 컬럼 이름입니다.
	 */
	public static List<Map<String, Object>> parseResultSet(List<String> rawResult) {
		List<Map<String, Object>> parsedData = new ArrayList<>();

		if (rawResult == null || rawResult.size() <= 1) {
			return parsedData;
		}

		String[] headers = rawResult.get(0).split("\t");

		for (int i = 1; i < rawResult.size(); i++) {
			String[] values = rawResult.get(i).split("\t");
			Map<String, Object> row = new HashMap<>();

			for (int j = 0; j < headers.length; j++) {
				if (j < values.length) {
					row.put(headers[j].toLowerCase(), values[j]);
				} else {
					row.put(headers[j].toLowerCase(), null);
				}
			}
			parsedData.add(row);
		}

		return parsedData;
	}

	/**
	 * INSERT INTO SQL 쿼리 문자열에서 테이블 이름을 추출합니다.
	 * 쿼리 문자열이 "INSERT INTO"로 시작하지 않거나 테이블 이름을 찾을 수 없는 경우 null을 반환합니다.
	 *
	 * @param sqlQuery 테이블 이름을 추출할 SQL INSERT 쿼리 문자열
	 * @return 추출된 테이블 이름 (트림된 형태) 또는 null
	 */
	public static String TableNameFromInsert(String sqlQuery) {
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            return null;
        }

        String normalizedSql = sqlQuery.trim().toUpperCase();

        if (!normalizedSql.startsWith("INSERT INTO")) {
            return null;
        }

        int startIndex = "INSERT INTO ".length();
        int endIndex = normalizedSql.indexOf(" ", startIndex);
        if (endIndex == -1) {
            endIndex = normalizedSql.indexOf("(", startIndex);
        }

        if (endIndex == -1) {
            return sqlQuery.substring(startIndex).trim();
        } else {
            return sqlQuery.substring(startIndex, endIndex).trim();
        }
	}
}
