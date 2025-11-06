import React, { createContext, useState, useContext, useCallback } from 'react';
import axiosInstance from '../api/axiosInstance';
import { AuthContext } from './AuthContext';
import { toast } from 'react-toastify';

const FriendContext = createContext();

function FriendProvider({ children }) {
    const { user } = useContext(AuthContext);

    const [friends, setFriends] = useState([]);

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
