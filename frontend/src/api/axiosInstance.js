import axios from 'axios';

/**
 * @file API 요청을 위한 Axios 인스턴스를 생성하고 설정하는 모듈입니다.
 * 이 인스턴스는 애플리케이션 전역에서 API 통신에 사용됩니다.
 */

/**
 * 백엔드 서버의 기본 URL 주소입니다.
 * @type {string}
 */
const SERVER_BASE_URL = 'https://simplechat-kopo307-app-bzeyf7e4hhcegbhy.koreacentral-01.azurewebsites.net';

/**
 * 전역으로 사용될 Axios 인스턴스입니다.
 * - `baseURL`: 모든 요청의 기본 URL을 서버 주소로 설정합니다.
 * - `withCredentials`: 모든 요청에 세션/쿠키 정보를 포함시켜 서버가 사용자를 인식할 수 있도록 합니다.
 * @type {import('axios').AxiosInstance}
 */
const axiosInstance = axios.create({
    baseURL: SERVER_BASE_URL,
    withCredentials: true, // 세션/쿠키 정보를 서버에 함께 보내기 위한 설정
});

// 다른 파일에서 이 설정을 가져다 쓸 수 있도록 export 합니다.
export default axiosInstance;