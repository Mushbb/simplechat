import React, { createContext, useState, useContext, useCallback } from 'react';
import axiosInstance from '../api/axiosInstance';
import { AuthContext } from './AuthContext';
import { toast } from 'react-toastify';

/**
 * @file 친구 목록 상태와 관련 함수를 전역적으로 관리하고 제공하는 컨텍스트입니다.
 */

/**
 * @typedef {object} Friend
 * @property {number} userId - 친구의 고유 ID.
 * @property {string} username - 친구의 사용자 이름.
 * @property {string} nickname - 친구의 닉네임.
 * @property {string} profileImageUrl - 친구의 프로필 이미지 URL.
 * @property {string} status - 친구 관계의 상태 (예: "ACCEPTED").
 * @property {'CONNECT' | 'DISCONNECT'} conn - 친구의 현재 접속 상태.
 */

/**
 * @typedef {object} FriendContextType
 * @property {Friend[]} friends - 현재 사용자의 친구 목록.
 * @property {React.Dispatch<React.SetStateAction<Friend[]>>} setFriends - 친구 목록 상태를 업데이트하는 함수.
 * @property {(friendId: number) => Promise<void>} removeFriend - 친구를 삭제하는 함수.
 */

/**
 * 친구 컨텍스트 객체입니다.
 * @type {React.Context<FriendContextType>}
 */
const FriendContext = createContext();

/**
 * 친구 관련 상태와 기능을 제공하는 React 컴포넌트입니다.
 * @param {object} props
 * @param {React.ReactNode} props.children - 이 Provider가 감쌀 자식 컴포넌트들.
 * @returns {JSX.Element} FriendContext.Provider
 */
function FriendProvider({ children }) {
    const { user } = useContext(AuthContext);

    /** @type {[Friend[], React.Dispatch<React.SetStateAction<Friend[]>>]} */
    const [friends, setFriends] = useState([]);

    /**
     * 친구 목록에서 특정 친구를 삭제합니다.
     * 사용자에게 확인을 받은 후, 서버에 삭제 요청을 보내고 로컬 상태를 업데이트합니다.
     * @param {number} friendId - 삭제할 친구의 ID.
     * @returns {Promise<void>}
     */
    const removeFriend = async (friendId) => {
        if (!window.confirm("정말로 친구를 삭제하시겠습니까?")) {
            return;
        }
        try {
            await axiosInstance.delete(`/api/friends/${friendId}`);
            setFriends(prevFriends => prevFriends.filter(f => f.userId !== friendId));
            toast.success("친구를 삭제했습니다.");
        } catch (error) {
            console.error("Failed to remove friend:", error);
            toast.error("친구 삭제에 실패했습니다.");
        }
    };

    const value = {
        friends,
        setFriends,
        removeFriend,
    };

    return (
        <FriendContext.Provider value={value}>
            {children}
        </FriendContext.Provider>
    );
}

export { FriendContext, FriendProvider };
