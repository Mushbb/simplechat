import React, { useState, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

// 간단한 리스트 아이템 스타일
const listItemStyle = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  padding: '15px',
  borderBottom: '1px solid #eee',
  listStyle: 'none'
};

const roomInfoStyle = {
  flex: 1,
};

const roomActionsStyle = {
  marginLeft: '20px',
};

function LobbyPage() {
  const [rooms, setRooms] = useState([]);
  const { user } = useContext(AuthContext);
  const navigate = useNavigate();

  // 방 입장 처리 함수 - async/await 추가
  const handleEnterRoom = async (room) => {
    let password = '';
    // 비공개 방일 경우 비밀번호를 입력받습니다.
    if (room.roomType === 'PRIVATE') {
      password = prompt('비밀번호를 입력하세요:');
      if (password === null) { // 사용자가 취소 버튼을 누른 경우
        return;
      }
    }

    try {
      const response = await fetch(`/room/${room.id}/users`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({ password }), // 비밀번호를 body에 담아 전송
      });

      if (response.ok) {
        console.log(`Successfully entered room ${room.id}`);
        navigate(`/chat/${room.id}`); // 성공 시에만 페이지 이동
      } else {
        const errorData = await response.json();
        alert(errorData.message || '입장에 실패했습니다.');
      }
    } catch (error) {
      console.error('Failed to enter room:', error);
      alert('방 입장에 실패했습니다.');
    }
  };

  useEffect(() => {
    if (user) {
      const fetchRooms = async () => {
        try {
          const response = await fetch('/room/list');
          if (response.ok) {
            const data = await response.json();
            setRooms(data);
          } else {
            console.error('Failed to fetch rooms');
          }
        } catch (error) {
          console.error('Error fetching rooms:', error);
        }
      };
      fetchRooms();
    }
  }, [user]);

  if (!user) {
    return <h2>로그인하고 채팅방 목록을 확인하세요.</h2>;
  }

  return (
    <div>
      <h2>로비 페이지</h2>
      <div id="room-list">
        <h3>채팅방 목록</h3>
        <ul style={{ padding: 0 }}>
          {rooms.length > 0 ? rooms.map(room => (
            <li key={room.id} style={listItemStyle}>
              <div style={roomInfoStyle}>
                <strong style={{ fontSize: '1.2em' }}>{room.name}</strong>
                <span style={{ marginLeft: '10px', color: '#888' }}>({room.roomType})</span>
                <div style={{ color: '#555', marginTop: '5px' }}>
                  <span>소유자: {room.ownerName}</span>
                  <span style={{ marginLeft: '15px' }}>접속인원: {room.connCount} / {room.userCount}</span>
                </div>
              </div>
              <div style={roomActionsStyle}>
                <button onClick={() => handleEnterRoom(room)}>입장</button>
              </div>
            </li>
          )) : <p>현재 생성된 채팅방이 없습니다.</p>}
        </ul>
      </div>
    </div>
  );
}

export default LobbyPage;
