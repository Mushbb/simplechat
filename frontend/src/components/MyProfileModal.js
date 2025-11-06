import React, { useState, useContext, useEffect, useRef } from 'react';
import { AuthContext } from '../context/AuthContext';
import { ModalContext } from '../context/ModalContext';
import axiosInstance from '../api/axiosInstance';
const SERVER_URL = axiosInstance.getUri();

function MyProfileModal() {
    const { user, updateUser } = useContext(AuthContext);
    const { closeProfileModal } = useContext(ModalContext);
    const [nickname, setNickname] = useState(user?.nickname || '');
    const [statusMessage, setStatusMessage] = useState(user?.status_message || '');
    const [newImageFile, setNewImageFile] = useState(null); // newImageFile 상태 추가
    const fileInputRef = useRef(null);

    useEffect(() => {
        setNickname(user?.nickname || '');
        setStatusMessage(user?.status_message || '');
    }, [user]);

    const handleImageChange = (e) => {
        if (e.target.files && e.target.files[0]) {
            setNewImageFile(e.target.files[0]);
        }
    };

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
                <form onSubmit={handleUpdate}> {/* onSubmit 핸들러를 handleUpdate로 변경 */}
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