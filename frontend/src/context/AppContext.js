import React from 'react';
import { AuthProvider } from './AuthContext';
import { WebSocketProvider } from './WebSocketContext';
import { NotificationProvider } from './NotificationContext';
import { ModalProvider } from './ModalContext';
import { FriendProvider } from './FriendContext';
import { RoomProvider } from './RoomContext';
import { ChatProvider } from './ChatContext';

/**
 * @file 애플리케이션에서 사용되는 모든 React Context Provider들을 하나로 조합하는 컴포넌트를 제공합니다.
 * 이를 통해 App.js에서 Provider들을 깔끔하게 관리할 수 있습니다.
 */

/**
 * 애플리케이션의 모든 컨텍스트 프로바이더를 담고 있는 배열입니다.
 * Provider들의 순서가 중요합니다.
 * 바깥쪽 Provider는 안쪽 Provider의 값에 접근할 수 없으므로,
 * 의존성이 없는 Provider를 배열의 앞쪽에, 의존성이 있는 Provider를 뒤쪽에 배치해야 합니다.
 * reduceRight로 조합되므로, 배열의 마지막 요소가 가장 바깥쪽 Provider가 됩니다.
 * @type {React.ElementType[]}
 */
const providers = [
  AuthProvider,
  RoomProvider,
  FriendProvider,
  WebSocketProvider,
  NotificationProvider,
  ChatProvider,
  ModalProvider,
];

/**
 * 모든 컨텍스트 프로바이더를 조합하여 자식 컴포넌트에게 제공하는 컴포넌트입니다.
 * @param {object} props
 * @param {React.ReactNode} props.children - 모든 컨텍스트를 제공받을 자식 컴포넌트들.
 * @returns {JSX.Element} 조합된 프로바이더들로 감싸진 자식 컴포넌트.
 */
export const AppContextProvider = ({ children }) => {
  return providers.reduceRight((acc, Provider) => {
    return <Provider>{acc}</Provider>;
  }, children);
};
