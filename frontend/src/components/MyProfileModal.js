import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import axiosInstance from '../api/axiosInstance';

const SERVER_URL = 'http://10.50.131.25:8080';

function MyProfileModal() {
    const { user, updateUser, closeProfileModal } = useContext(AuthContext);

    const [nickname, setNickname] = useState('');
    const [statusMessage, setStatusMessage] = useState('');
    const [profileImageFile, setProfileImageFile] = useState(null);
    const [previewImageUrl, setPreviewImageUrl] = useState('');

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const response = await axiosInstance.get(`/user/${user.userId}/profile`);
                const profile = response.data;
                setNickname(profile.nickname);
                setStatusMessage(profile.status_msg || '');
                setPreviewImageUrl(`${SERVER_URL}${profile.imageUrl}`);
            } catch (error) { console.error("Failed to fetch profile", error); }
        };
        if (user) { fetchProfile(); }
    }, [user]);

    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setProfileImageFile(file);
            const reader = new FileReader();
            reader.onloadend = () => { setPreviewImageUrl(reader.result); };
            reader.readAsDataURL(file);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        await updateUser(nickname, statusMessage, profileImageFile);
    };

    return (
        <div className="modal-overlay" onClick={closeProfileModal}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close-btn" onClick={closeProfileModal}>&times;</button>
                <h2>프로필 수정</h2>
                <form onSubmit={handleSubmit}>
                    <div className="profile-image-area">
                        <img src={previewImageUrl} alt="Profile preview" className="profile-image-preview" />
                        <label htmlFor="profile-image-input" className="profile-image-label">사진 변경</label>
                        <input
                            id="profile-image-input"
                            type="file"
                            accept="image/*"
                            onChange={handleImageChange}
                            style={{ display: 'none' }}
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="profile-nickname">닉네임</label>
                        <input id="profile-nickname" type="text" value={nickname} onChange={(e) => setNickname(e.target.value)} />
                    </div>
                    <div className="form-group">
                        <label htmlFor="profile-status">상태 메시지</label>
                        <textarea id="profile-status" rows="3" value={statusMessage} onChange={(e) => setStatusMessage(e.target.value)} />
                    </div>
                    <button type="submit" className="modal-submit-btn">저장하기</button>
                </form>
            </div>
        </div>
    );
}

export default MyProfileModal;