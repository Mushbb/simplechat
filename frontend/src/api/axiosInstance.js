import axios from 'axios';

// Spring Boot 서버 주소
const SERVER_BASE_URL = 'https://simplechat-kopo307-app-bzeyf7e4hhcegbhy.koreacentral-01.azurewebsites.net';

const axiosInstance = axios.create({
    baseURL: SERVER_BASE_URL,
    withCredentials: true, // 세션/쿠키 정보를 서버에 함께 보내기 위한 설정
});

// 다른 파일에서 이 설정을 가져다 쓸 수 있도록 export 합니다.
export default axiosInstance;