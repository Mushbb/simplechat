import React from 'react';
import ReactDOM from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ChatProvider } from './context/ChatContext';
import { NotificationProvider } from './context/NotificationContext';
import { FriendProvider } from './context/FriendContext';
import { ModalProvider } from './context/ModalContext';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';

const root = ReactDOM.createRoot(document.getElementById('root'));
document.title = "SimpleChat";
root.render(
  <React.StrictMode>
    <AuthProvider>
        <FriendProvider>
            <ModalProvider>
                <ChatProvider>
                    <NotificationProvider>
                      <HashRouter>
                        <App />
                      </HashRouter>
                    </NotificationProvider>
                </ChatProvider>
            </ModalProvider>
        </FriendProvider>
    </AuthProvider>
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
