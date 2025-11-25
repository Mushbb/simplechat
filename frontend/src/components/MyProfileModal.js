import React, { useState, useContext, useEffect, useRef } from 'react';
import { AuthContext } from '../context/AuthContext';
import { ModalContext } from '../context/ModalContext';
import axiosInstance from '../api/axiosInstance';

/**
 * @file 현재 로그인한 사용자의 프로필(닉네임, 상태 메시지, 프로필 이미지)을 수정하는 모달 컴포넌트입니다.
 */

const SERVER_URL = axiosInstance.getUri();

/**
 * 내 프로필 수정 모달 컴포넌트.
 * @returns {JSX.Element} MyProfileModal 컴포넌트의 JSX.
 */
function MyProfileModal() {
    const { user, updateUser } = useContext(AuthContext);
    const { closeProfileModal } = useContext(ModalContext);

    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 닉네임 입력 상태 */
    const [nickname, setNickname] = useState(user?.nickname || '');
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 상태 메시지 입력 상태 */
    const [statusMessage, setStatusMessage] = useState(user?.status_message || '');
    /** @type {[File|null, React.Dispatch<React.SetStateAction<File|null>>]} 새로 선택된 프로필 이미지 파일 상태 */
    const [newImageFile, setNewImageFile] = useState(null);
    /** @type {React.RefObject<HTMLInputElement>} 파일 입력(input) 엘리먼트에 대한 Ref */
    const fileInputRef = useRef(null);

    /**
     * AuthContext의 user 객체가 변경될 때, 모달의 내부 상태(닉네임, 상태메시지)를 동기화하는 Effect.
     */
    useEffect(() => {
        setNickname(user?.nickname || '');
        setStatusMessage(user?.status_message || '');
    }, [user]);

    /**
     * 사용자가 새로운 프로필 이미지를 선택했을 때 호출되는 핸들러.
     * @param {React.ChangeEvent<HTMLInputElement>} e - 파일 입력 변경 이벤트.
     */
    const handleImageChange = (e) => {
        if (e.target.files && e.target.files[0]) {
            setNewImageFile(e.target.files[0]);
        }
    };

    /**
     * '저장' 버튼 클릭 시 폼 제출을 처리하는 핸들러.
     * `updateUser` 컨텍스트 함수를 호출하여 프로필 정보를 업데이트합니다.
     * @param {React.FormEvent} e - 폼 제출 이벤트.
     */
    const handleUpdate = async (e) => {
        e.preventDefault();
        await updateUser(nickname, statusMessage, newImageFile);
        closeProfileModal();
    };

    return (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && closeProfileModal()}>
            <div className="modal-content">
                <button className="modal-close-btn" onClick={closeProfileModal}>&times;</button>
                <h2>프로필 수정</h2>
                <form onSubmit={handleUpdate}>
                    <div className="form-group profile-image-area">
                        <img
                            src={newImageFile ? URL.createObjectURL(newImageFile) : (user?.profile_image_url || '/images/profiles/default.png')}
                            alt="프로필 이미지"
                            className="profile-image-preview"
                        />
                        <input
                            type="file"
                            ref={fileInputRef}
                            onChange={handleImageChange}
                            style={{ display: 'none' }}
                            accept="image/*"
                        />
                        <label onClick={() => fileInputRef.current.click()} className="profile-image-label">
                            이미지 변경
                        </label>
                    </div>
                    <div className="form-group">
                        <label htmlFor="nickname">닉네임</label>
                        <input
                            type="text"
                            id="nickname"
                            value={nickname}
                            onChange={(e) => setNickname(e.target.value)}
                            maxLength={10}
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="statusMessage">상태 메시지</label>
                        <textarea
                            id="statusMessage"
                            value={statusMessage}
                            onChange={(e) => setStatusMessage(e.target.value)}
                            maxLength={50}
                        />
                    </div>
                    <button type="submit" className="modal-submit-btn">저장</button>
                </form>
            </div>
        </div>
    );
}

export default MyProfileModal;